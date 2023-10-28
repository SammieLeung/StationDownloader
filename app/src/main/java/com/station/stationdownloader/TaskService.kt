package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.logger.Logger
import com.station.stationdownloader.TaskId.Companion.INVALID_ID
import com.station.stationdownloader.TaskId.Companion.INVALID_TASK_ID
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.data.source.remote.json.RemoteStartTask
import com.station.stationdownloader.data.source.remote.json.RemoteStopTask
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.data.usecase.EngineRepoUseCase
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
    lateinit var engineRepo: DefaultEngineRepository

    @Inject
    lateinit var engineRepoUseCase: EngineRepoUseCase

    @Inject
    lateinit var taskRepo: IDownloadTaskRepository

    var taskStatusBinder: TaskStatusServiceImpl? = null

    private val watchTaskJobMap = mutableMapOf<String, Job>()

    //开始任务 job，用于防止重复点击开始任务
    private var startTaskJobMap = mutableMapOf<String, Job>()

    //停止任务 job，用于防止重复点击停止任务
    private var stopTaskJobMap = mutableMapOf<String, Job>()

    //任务状态流
    private val _taskStatusFlow: MutableStateFlow<TaskStatus> =
        MutableStateFlow(
            TaskStatus(
                taskId = INVALID_TASK_ID,
                url = "",
                speed = 0,
                downloadSize = 0,
                totalSize = 0,
                status = 0
            )
        )

    private val downloadingTasks: MutableMap<String, TaskStatus> = mutableMapOf()


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
                    val taskId = intent.getStringExtra("taskId") ?: return START_NOT_STICKY
                    if (taskId == INVALID_ID)
                        return START_NOT_STICKY
                    val taskEngineName = intent.getStringExtra("engine") ?: return START_NOT_STICKY
                    if (taskEngineName == DownloadEngine.INVALID_ENGINE.name)
                        return START_NOT_STICKY
                    watchTaskJobMap[url]?.apply { cancel() }
                    watchTaskJobMap[url] =
                        createWatchTask(
                            TaskId(
                                taskEngineName.let { DownloadEngine.valueOf(it) },
                                taskId
                            ), url
                        )
                }

                ACTION_CANCEL_WATCH_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    downloadingTasks.remove(url)
                    watchTaskJobMap.remove(url)?.apply { cancel() }
                }

                ACTION_CANCEL_ALL_ARIA2_WATCH_TASK -> {
                    watchTaskJobMap.filter {
                        downloadingTasks[it.key]?.taskId?.engine == DownloadEngine.ARIA2
                    }.forEach {
                        watchTaskJobMap.remove(it.key).apply {
                            this?.cancel()
                        }
                    }
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
            if (downloadingTasks[url] != null)
                stopTask(url).join()
            val downloadTask = xlEntity.asStationDownloadTask()
            val taskIdResult = engineRepo.startTask(downloadTask)
            if (taskIdResult is IResult.Error) {
                Logger.e(taskIdResult.exception.message.toString())
                sendLocalBroadcast(Intent(
                    ACTION_START_TASK_RESULT
                ).apply {
                    putExtra("url", url)
                    putExtra("result", false)
                    putExtra("reason", taskIdResult.exception.message.toString())
                })
                updateTaskStatusNow(url, TaskId(downloadTask.engine, ""), ITaskState.STOP.code)
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
            downloadingTasks[url] = status
            updateTaskStatusNow(url, taskId, ITaskState.RUNNING.code)
            sendToClient(
                "start_task",
                MoshiHelper.toJson(RemoteStartTask(url, taskId.id))
            )

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
                val taskId = downloadingTasks[url]?.taskId
                if (taskId == null) {
                    taskRepo.updateTask(entity.copy(status = DownloadTaskStatus.PAUSE))
                } else {
                    engineRepo.stopTask(taskId, entity.asStationDownloadTask())
                    sendToClient("stop_task", MoshiHelper.toJson(RemoteStopTask(url)))
                }
                taskId?.let { updateTaskStatusNow(url, it, ITaskState.STOP.code) }
            } catch (e: Exception) {
                logError(e.message.toString())
                sendErrorToClient(
                    "stop_task",
                    e.message.toString(),
                    TaskExecuteError.STOP_TASK_FAILED.ordinal
                )
            } finally {
                stopTaskJobMap.remove(url)
                downloadingTasks.remove(url)
                if (downloadingTasks[url] == null && watchTaskJobMap[url] == null && stopTaskJobMap[url] == null) {
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
            val entity = taskRepo.getTaskByUrl(url)
            if (entity == null) {
                sendErrorToClient(
                    "remove_task",
                    "Task not found!",
                    TaskExecuteError.STOP_TASK_FAILED.ordinal
                )
                return@launch
            }
            //TODO 优化不同引擎的删除任务逻辑。XL引擎删除任务时，会自动停止任务，所以这里先停止任务，再删除任务。ARI2引擎删除任务时，不会自动停止任务，所以这里不需要停止任务，直接删除任务即可。
            stopTask(url)
            if (entity.engine == DownloadEngine.ARIA2) {
                engineRepo.removeAria2Task(url)
            }
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


    private fun createWatchTask(taskId: TaskId, url: String): Job {
        return serviceScope.launch {
            try {
                when (taskId.engine) {
                    DownloadEngine.XL -> {
                        speedTest(taskId, url)
                    }

                    DownloadEngine.ARIA2 -> {
                        aria2TellStatus(taskId, url)
                    }

                    DownloadEngine.INVALID_ENGINE -> TODO()
                }

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
        taskId: TaskId, url: String, failedCount: Int = 0, delayTestTime: Int = 30
    ) {
        if (taskId.engine != DownloadEngine.XL) return
        val task: XLDownloadTaskEntity = taskRepo.getTaskByUrl(url) ?: return
        var delayCount = delayTestTime
        var nullCount = 0
        var noSpeedCount = 0
        var startSpeedTest = false
        var retryFailedCount = failedCount
        var isRestart = false
        val isStopRetry = retryFailedCount >= 5

        while (true) {
            val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId.id.toLong())
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
                downloadingTasks.remove(url)
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

    private suspend fun retryDownload(oldTaskId: TaskId, url: String, retryCount: Int) {
        logger("重试下载【$url】")
        Logger.e("重试下载 原[$oldTaskId] 第[${retryCount + 1}]次重试 下次重试间隔【${(retryCount + 1) * 5}】")
        val xlTaskEntity = taskRepo.getTaskByUrl(url) ?: return
        val stationDownloadTask = xlTaskEntity.asStationDownloadTask()
        val result = engineRepo.restartTask(oldTaskId, stationDownloadTask)
        if (result is IResult.Error) {
            Logger.e(result.exception.message.toString())
            return
        }
        val taskResult = (result as IResult.Success).data
        speedTest(taskResult, url, retryCount + 1, (retryCount + 1) * 5)
    }

    private suspend fun aria2TellStatus(taskId: TaskId, url: String) {
        while (true) {
            val taskStatusResult = engineRepo.getAria2TaskStatus(taskId.id, url)
            if (taskStatusResult is IResult.Error) {
                logError(taskStatusResult.exception)
                return
            }
            val taskStatus = (taskStatusResult as IResult.Success).data
            updateTaskStatus(url, taskStatus)
            if (taskStatus.status == ITaskState.DONE.code || taskStatus.status == ITaskState.STOP.code) {
                if (taskStatus.status == ITaskState.DONE.code) {
                    Logger.w("下载完成 [${taskId}]")
                    engineRepo.stopTask(
                        taskId,
                        taskRepo.getTaskByUrl(url)?.asStationDownloadTask() ?: return
                    )
                }
                downloadingTasks.remove(url)
                watchTaskJobMap.remove(url)?.cancel()
                return
            }
            delay(1000)
        }
    }

    private var shouldStop = false
    private suspend fun aria2TellAll() {
        while (!shouldStop) {
            val list = engineRepo.tellAll()
            list.forEach {

                updateTaskStatus(it.taskStatus.url, it.taskStatus)
            }
        }
    }

    private fun updateTaskStatusNow(url: String, taskId: TaskId, status: Int) {
        when (taskId.engine) {
            DownloadEngine.XL -> {
                val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId.id.toLong()) ?: return
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

            DownloadEngine.ARIA2 -> {
                serviceScope.launch {
                    updateTaskStatus(url, TaskStatus(taskId, url, 0, 0, 0, status))
                }
            }

            DownloadEngine.INVALID_ENGINE -> TODO()
        }

    }

    private fun updateTaskStatus(url: String, status: TaskStatus) {
        downloadingTasks[url] = status
        _taskStatusFlow.update {
            status
        }
    }

    private fun stopWatchStatus(url: String) {
        watchTaskJobMap.remove(url)?.cancel()
        if (watchTaskJobMap.isEmpty()) stopSelf()
    }

    fun asTaskStatusStateFlow(): StateFlow<TaskStatus> {
        return _taskStatusFlow.asStateFlow()
    }

    fun getRunningTaskMap(): MutableMap<String, TaskStatus> {
        return downloadingTasks
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
        private const val ACTION_ARIA2_WATCH_TASK = "action.aria2.watch.task"

        private const val ACTION_CANCEL_WATCH_TASK = "action.cancel.watch.task"
        private const val ACTION_CANCEL_ARIA2_WATCH_TASK = "action.cancel.aria2.watch.task"
        private const val ACTION_CANCEL_ALL_ARIA2_WATCH_TASK = "action.cancel.all.aria2.watch.task"

        private const val ACTION_START_TASK = "action.start.task"
        private const val ACTION_STOP_TASK = "action.stop.task"

        private const val ACTION_DELETE_TASK = "action.delete.task"

        const val ACTION_START_TASK_RESULT = "action.start.task.result"
        const val ACTION_DELETE_TASK_RESULT = "action.delete.task.result"


        @JvmStatic
        fun watchTask(context: Context, url: String, taskId: String, engine: String) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_WATCH_TASK)
            intent.putExtra("url", url)
            intent.putExtra("taskId", taskId)
            intent.putExtra("engine", engine)
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
        fun cancelAllWatchTask(context: Context) {
            val intent =
                Intent(context, TaskService::class.java).setAction(
                    ACTION_CANCEL_ALL_ARIA2_WATCH_TASK
                )
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

