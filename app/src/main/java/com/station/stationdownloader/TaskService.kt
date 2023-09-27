package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.logger.Logger
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.data.source.remote.json.RemoteStartTask
import com.station.stationdownloader.data.source.remote.json.RemoteStopTask
import com.station.stationdownloader.data.util.EngineRepoUseCase
import com.station.stationdownloader.di.AppCoroutineScope
import com.station.stationdownloader.utils.DLogger
import com.station.stationkitkt.MoshiHelper
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskService : Service(), DLogger {
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    @AppCoroutineScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var engineRepo: IEngineRepository

    @Inject
    lateinit var engineRepoUseCase:EngineRepoUseCase

    @Inject
    lateinit var taskRepo: IDownloadTaskRepository

    var taskStatusBinder: TaskStatusServiceImpl? = null

    private val watchTaskJobMap = mutableMapOf<String, Job>()
    private var startTaskJobMap = mutableMapOf<String, Job>()
    private var stopTaskJobMap = mutableMapOf<String, Job>()
    private val taskStatusFlow: MutableStateFlow<TaskStatus> =
        MutableStateFlow(
            TaskStatus(
                taskId = 0,
                url = "",
                speed = 0,
                downloadSize = 0,
                totalSize = 0,
                status = 0
            )
        )

    private val runningTaskMap: MutableMap<String, TaskStatus> = mutableMapOf()

    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            if (application == null)
                return@launch
            val app = application as StationDownloaderApp
            if (!app.isInitialized()) {
                app.initialize()
                engineRepoUseCase.initEngine()
            }
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (intent.action) {
                ACTION_WATCH_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    val taskId = intent.getLongExtra("taskId", -1)
                    watchTaskJobMap[url]?.apply { cancel() }
                    watchTaskJobMap[url] = createWatchTask(taskId, url)
                }

                ACTION_CANCEL_WATCH_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    runningTaskMap.remove(url)
                    watchTaskJobMap.remove(url)?.apply { cancel() }
                }

                ACTION_START_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    startTaskJobMap[url] = startTask(url)
                }

                ACTION_STOP_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    stopTaskJobMap[url] = stopTask(url)
                }

                ACTION_DELETE_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    val isDeleteFile = intent.getBooleanExtra("isDeleteFile", false)
                    deleteTask(url, isDeleteFile)
                }

                else -> {
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? {
        if (taskStatusBinder == null)
            taskStatusBinder = TaskStatusServiceImpl(this, serviceScope)
        return taskStatusBinder
    }


    override fun onDestroy() {
        super.onDestroy()
        watchTaskJobMap.values.forEach { it.cancel() }
        startTaskJobMap.values.forEach { it.cancel() }
        stopTaskJobMap.values.forEach { it.cancel() }
        serviceScope.cancel()
    }


    private fun startTask(url: String) = serviceScope.launch {
        try {
            val xlEntity = taskRepo.getTaskByUrl(url) ?: return@launch
            if (runningTaskMap[url] != null)
                stopTask(url).join()
            val taskIdResult = engineRepo.startTask(xlEntity.asStationDownloadTask())
            if (taskIdResult is IResult.Error) {
                Logger.e(taskIdResult.exception.message.toString())
                sendLocalBroadcast(Intent(
                    ACTION_START_TASK_RESULT
                ).apply {
                    putExtra("url", url)
                    putExtra("result", false)
                    putExtra("reason", taskIdResult.exception.message.toString())
                })
                updateTaskStatusNow(url, -1, ITaskState.STOP.code)
                sendErrorToClient(
                    "start_task",
                    taskIdResult.exception.message.toString(),
                    TaskExecuteError.START_TASK_FAILED.ordinal
                )
                return@launch
            }
            val taskId = (taskIdResult as IResult.Success).data
            val status = TaskStatus(
                taskId = taskId,
                url = url,
                speed = 0,
                downloadSize = 0,
                totalSize = 0,
                status = ITaskState.UNKNOWN.code
            )
            runningTaskMap[url] = status
            updateTaskStatusNow(url, taskId, ITaskState.RUNNING.code)
            sendToClient("start_task", MoshiHelper.toJson(RemoteStartTask(url, taskId)))

            watchTaskJobMap[url]?.apply { cancel() }
            watchTaskJobMap[url] = createWatchTask(taskId, url)
        } catch (e: Exception) {
            logError(e.message.toString())
            sendErrorToClient(
                "start_task",
                e.message.toString(),
                TaskExecuteError.START_TASK_FAILED.ordinal
            )
        } finally {
            startTaskJobMap.remove(url)
        }
    }

    fun stopTask(url: String) =
        serviceScope.launch {
            try {
                watchTaskJobMap.remove(url)?.apply { cancel() }
                val entity = taskRepo.getTaskByUrl(url)
                if (entity == null) {
                    sendErrorToClient(
                        "stop_task",
                        "Download task not found!",
                        TaskExecuteError.STOP_TASK_FAILED.ordinal
                    )
                    return@launch
                }
                val taskId = runningTaskMap[url]?.taskId
                if (taskId == null) {
                    taskRepo.updateTask(entity.copy(status = DownloadTaskStatus.PAUSE))
                } else {
                    engineRepo.stopTask(taskId, entity.asStationDownloadTask())
                    sendToClient("stop_task", MoshiHelper.toJson(RemoteStopTask(url)))
                }
                updateTaskStatusNow(url, -1, ITaskState.STOP.code)
            } catch (e: Exception) {
                logError(e.message.toString())
                sendErrorToClient(
                    "stop_task",
                    e.message.toString(),
                    TaskExecuteError.STOP_TASK_FAILED.ordinal
                )
            } finally {
                stopTaskJobMap.remove(url)
                runningTaskMap.remove(url)
                if (runningTaskMap[url] == null && watchTaskJobMap[url] == null && stopTaskJobMap[url] == null) {
                    logger("stopTask finally clear!")
                }
            }

        }

    fun deleteTask(
        url: String,
        isDeleteFile: Boolean,
        callback: ITaskServiceCallback? = null
    ) =
        serviceScope.launch {
            stopTask(url)
            val deleteResult = taskRepo.deleteTask(url, isDeleteFile)
            if (deleteResult is IResult.Error) {
                Logger.e(deleteResult.exception.message.toString())
                callback?.onFailed(
                    "delete task 【$url】failed",
                    TaskExecuteError.DELETE_TASK_FAILED.ordinal
                )
                sendLocalBroadcast(
                    Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                        .putExtra("result", false)
                )
            } else {
                deleteResult as IResult.Success
                callback?.onResult(url)
                sendLocalBroadcast(
                    Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                        .putExtra("result", true)
                )
            }
        }


    private fun createWatchTask(taskId: Long, url: String): Job {
        return serviceScope.launch {
            try {
                speedTest(taskId, url)
            } finally {
                logger("【$url】")
                logger("need to cancel WatchJob")
            }
        }
    }


    /**
     *
     * @param taskId Long
     * @param url String
     * @param failedCount Int speedTest
     * @param delayTestTime Int
     */
    private suspend fun speedTest(
        taskId: Long, url: String, failedCount: Int = 0, delayTestTime: Int = 30
    ) {
        val task: XLDownloadTaskEntity = taskRepo.getTaskByUrl(url) ?: return
        var delayCount = delayTestTime
        var nullCount = 0
        var noSpeedCount = 0
        var startSpeedTest = false
        var retryFailedCount = failedCount
        var isRestart = false
        val isStopRetry = retryFailedCount >= 5



        while (true) {
            val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
            if (taskInfo == null) {
                if (nullCount < 5) {
                    nullCount++
                    delay(1000)
                    continue
                }
                break
            }
            nullCount = 0
            val speed = taskInfo.mDownloadSpeed
            val status = taskInfo.mTaskStatus
            updateTaskStatus(
                url,
                TaskStatus(
                    taskId = taskId,
                    url = url,
                    speed = taskInfo.mDownloadSpeed,
                    downloadSize = taskInfo.mDownloadSize,
                    totalSize = taskInfo.mFileSize,
                    status = status
                )
            )
            if (delayCount > 0) {
                delayCount--
            } else {
                startSpeedTest = true
            }

            val taskStatus = when (status) {
                ITaskState.RUNNING.code -> DownloadTaskStatus.DOWNLOADING
                ITaskState.STOP.code -> DownloadTaskStatus.PAUSE
                ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
                else -> DownloadTaskStatus.FAILED
            }
            taskRepo.updateTask(
                task.copy(
                    downloadSize = taskInfo.mDownloadSize,
                    totalSize = taskInfo.mFileSize,
                    status = taskStatus
                )
            )

            if (speed <= 0) {
                if (startSpeedTest) noSpeedCount++
            } else {
                delayCount = 0
                retryFailedCount = 0
                noSpeedCount = 0
            }

            if (status == ITaskState.DONE.code || status == ITaskState.STOP.code) {
                if (status == ITaskState.DONE.code) {
                    Logger.w("下载完成 [${taskId}]")
                    engineRepo.stopTask(taskId, task.asStationDownloadTask())
                }
                runningTaskMap.remove(url)
                watchTaskJobMap.remove(url)?.cancel()
                return
            }

            if (status == ITaskState.FAILED.code) {
                isRestart = true
                retryFailedCount = 0
            }

            if (noSpeedCount >= 5) {
                isRestart = true
                break
            }
            delay(1000)
        }

        delay(1000)
        if (isStopRetry) {
            Logger.e("重试次数过多，停止重试")
            stopTask(url)
            return
        }
        if (isRestart) retryDownload(taskId, url, retryFailedCount)
    }

    private suspend fun retryDownload(oldTaskId: Long, url: String, retryCount: Int) {
        logger("重试下载【$url】")
        Logger.e("重试下载 原[$oldTaskId] 第[${retryCount + 1}]次重试 下次重试间隔【${(retryCount + 1) * 5}】")
        val xlTaskEntity = taskRepo.getTaskByUrl(url) ?: return
        val stationDownloadTask = xlTaskEntity.asStationDownloadTask()
        val result = engineRepo.restartTask(oldTaskId, stationDownloadTask)
        if (result is IResult.Error) {
            Logger.e(result.exception.message.toString())
            return
        }
        val taskId = (result as IResult.Success).data
        speedTest(taskId, url, retryCount + 1, (retryCount + 1) * 5)
    }

    private fun updateTaskStatusNow(url: String, taskId: Long, status: Int) {
        val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId) ?: return
        val taskStatus = TaskStatus(
            taskId = taskId,
            url = url,
            speed = taskInfo.mDownloadSpeed,
            downloadSize = taskInfo.mDownloadSize,
            totalSize = taskInfo.mFileSize,
            status = status
        )
        updateTaskStatus(url, taskStatus)
    }

    private fun updateTaskStatus(url: String, status: TaskStatus) {
        runningTaskMap[url] = status
        taskStatusFlow.update {
            status
        }
    }

    private fun stopWatchStatus(url: String) {
        watchTaskJobMap.remove(url)?.cancel()
        if (watchTaskJobMap.isEmpty()) stopSelf()
    }

    fun getStatusFlow(): StateFlow<TaskStatus> {
        return taskStatusFlow.asStateFlow()
    }

    fun getRunningTaskMap(): MutableMap<String, TaskStatus> {
        return runningTaskMap
    }

    private fun sendLocalBroadcast(intent: Intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendToClient(command: String, data: String) {
        taskStatusBinder?.sendToClient(command, data)
    }

    private fun sendErrorToClient(command: String, reason: String, code: Int) {
        taskStatusBinder?.sendErrorToClient(command, reason, code)
    }

    override fun DLogger.tag(): String {
        return TaskService.javaClass.simpleName
    }

    companion object {
        private const val ACTION_WATCH_TASK = "action.watch.task"
        private const val ACTION_CANCEL_WATCH_TASK = "action.cancel.watch.task"

        private const val ACTION_START_TASK = "action.start.task"
        private const val ACTION_STOP_TASK = "action.stop.task"

        private const val ACTION_DELETE_TASK = "action.delete.task"

        const val ACTION_START_TASK_RESULT = "action.start.task.result"
        const val ACTION_DELETE_TASK_RESULT = "action.delete.task.result"

        @JvmStatic
        fun watchTask(context: Context, url: String, taskId: Long = -1) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_WATCH_TASK)
            intent.putExtra("url", url)
            intent.putExtra("taskId", taskId)
            context.startService(intent)
        }

        @JvmStatic
        fun cancelWatchTask(context: Context, url: String) {
            val intent =
                Intent(context, TaskService::class.java).setAction(ACTION_CANCEL_WATCH_TASK)
            intent.putExtra("url", url)
            context.startService(intent)
        }


        @JvmStatic
        fun startTask(context: Context, url: String) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_START_TASK)
            intent.putExtra("url", url)
            context.startService(intent)
        }

        @JvmStatic
        fun stopTask(context: Context, url: String) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_STOP_TASK)
            intent.putExtra("url", url)
            context.startService(intent)
        }

        @JvmStatic
        fun deleteTask(context: Context, url: String, isDeleteFile: Boolean) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_DELETE_TASK)
            intent.putExtra("url", url)
            intent.putExtra("isDeleteFile", isDeleteFile)
            context.startService(intent)
        }
    }
}

