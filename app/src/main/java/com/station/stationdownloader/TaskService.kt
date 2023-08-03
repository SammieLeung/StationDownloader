package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.logger.Logger
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.di.AppCoroutineScope
import com.station.stationdownloader.utils.DLogger
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
import java.util.concurrent.atomic.AtomicBoolean
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
    lateinit var taskRepo: IDownloadTaskRepository

    @Inject
    lateinit var configRepo: IConfigurationRepository

    private val watchTaskJobMap = mutableMapOf<String, Job>()
    private var startTaskJobMap = mutableMapOf<String, Job>()
    private var stopTaskJobMap = mutableMapOf<String, Job>()

    private val taskAtomMap= mutableMapOf<String,AtomicBoolean>()

    private val taskStatusFlow: MutableStateFlow<TaskStatus> =
        MutableStateFlow(TaskStatus(0, "", 0, 0, 0, 0))

    private val runningTaskMap: MutableMap<String, TaskStatus> = mutableMapOf()

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
                    if(taskAtomMap[url]?.get() == true){
                        logger("double click start task $url")
                        return START_NOT_STICKY
                    }
                    taskAtomMap[url]=AtomicBoolean(true)
                    logger("startTas taskAtomMap=${taskAtomMap[url]}")

                    startTaskJobMap[url] = startTask(url)
                }

                ACTION_STOP_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    if(taskAtomMap[url]?.get() == true) {
                        logger("double click stop task $url")
                        return START_NOT_STICKY
                    }
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
        return TaskBinder()
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
            logger("startTask $url")
            val xlEntity = taskRepo.getTaskByUrl(url) ?: return@launch
            if (watchTaskJobMap.size == configRepo.getMaxThread()) {
                sendLocalBroadcast(Intent(
                    ACTION_START_TASK_RESULT
                ).apply {
                    putExtra("url", url)
                    putExtra("result", false)
                    putExtra("reason", TaskExecuteError.START_TASK_MAX_LIMIT.name)
                })
                updateTaskStatusNow(url, -1, ITaskState.STOP.code)
                return@launch
            }
            val taskIdResult = engineRepo.startTask(xlEntity.asStationDownloadTask())
            if (taskIdResult is IResult.Error) {
                sendLocalBroadcast(Intent(
                    ACTION_START_TASK_RESULT
                ).apply {
                    putExtra("url", url)
                    putExtra("result", false)
                    putExtra("reason", taskIdResult.exception.message.toString())
                })
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

            watchTaskJobMap[url]?.apply { cancel() }
            watchTaskJobMap[url] = createWatchTask(taskId, url)
            logger("startTask over $url")
        } catch (e: Exception) {
            logError(e.message.toString())
        } finally {

            startTaskJobMap.remove(url)
            taskAtomMap[url]?.set(false)
            logger("startTask finallly $url")
        }
    }

    private fun stopTask(url: String) = serviceScope.launch {
        try {
            logger("stopTask $url")
            watchTaskJobMap.remove(url)?.apply { cancel() }
            val entity = taskRepo.getTaskByUrl(url) ?: return@launch
            val taskId = runningTaskMap.remove(url)?.taskId
            if (taskId == null) {
                taskRepo.updateTask(entity.copy(status = DownloadTaskStatus.PAUSE))
            } else {
                engineRepo.stopTask(taskId, entity.asStationDownloadTask())
            }
            updateTaskStatusNow(url, -1, ITaskState.STOP.code)
            logger("stopTask over $url")
        } catch (e: Exception) {
            logError(e.message.toString())
        } finally {
            stopTaskJobMap.remove(url)
            taskAtomMap[url]?.set(false)
            logger("stopTask finallly $url" )
        }

    }

    private fun deleteTask(url: String, isDeleteFile: Boolean) =
        serviceScope.launch {
            stopTask(url)
            val deleteResult = taskRepo.deleteTask(url, isDeleteFile)
            if (deleteResult is IResult.Error) {
                Logger.e(deleteResult.exception.message.toString())
                sendLocalBroadcast(
                    Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                        .putExtra("result", false)
                )
            } else {
                deleteResult as IResult.Success
                sendLocalBroadcast(
                    Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                        .putExtra("result", true)
                )
            }
        }

    private fun cancelJob(url: String) {
        startTaskJobMap[url]?.cancel()
        stopTaskJobMap[url]?.cancel()
    }

    private fun createWatchTask(taskId: Long, url: String): Job {
        return serviceScope.launch {
            try {
                speedTest(taskId, url)
            } finally {
                logger("need to cancel WatchJob, taskId: $taskId")
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
        val isStopRetry = retryFailedCount > 10
        var isRestart = false
        if (isStopRetry) return

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

        if (isRestart) restartTask(taskId, url, retryFailedCount)
    }

    private suspend fun restartTask(oldTaskId: Long, url: String, retryCount: Int) {
        val xlTaskEntity = taskRepo.getTaskByUrl(url) ?: return
        val stationDownloadTask = xlTaskEntity.asStationDownloadTask()
        val result = engineRepo.restartTask(oldTaskId, stationDownloadTask)
        if (result is IResult.Error) return
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

    override fun DLogger.tag(): String {
        return TaskService.javaClass.simpleName
    }


    // Binder 类用于提供任务状态信息给绑定的组件
    inner class TaskBinder : ITaskStatusService.Stub() {
        fun getService(): TaskService = this@TaskService
        override fun getTaskStatus(): MutableMap<String, TaskStatus> {
            val statusMap = mutableMapOf<String, TaskStatus>()
            runningTaskMap.forEach { (url, status) ->
                statusMap[url] = status.copy()
            }
            return statusMap
        }

        override fun getTaskStatusByUrl(url: String?): TaskStatus? {
            return runningTaskMap[url]?.copy()
        }
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

