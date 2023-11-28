package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.logger.Logger
import com.station.stationdownloader.TaskId.Companion.INVALID_ID
import com.station.stationdownloader.TaskId.Companion.INVALID_TASK_ID
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.result
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.data.source.remote.json.RemoteDeleteTask
import com.station.stationdownloader.data.source.remote.json.RemoteStartTask
import com.station.stationdownloader.data.source.remote.json.RemoteStopTask
import com.station.stationdownloader.data.source.remote.json.RemoteTaskStatus
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.data.succeeded
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
class TaskService : Service(), DLogger, WebSocketClient.OnNotify {

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    @AppCoroutineScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var engineRepo: DefaultEngineRepository

    @Inject
    lateinit var torrentRepo: ITorrentInfoRepository

    @Inject
    lateinit var engineRepoUseCase: EngineRepoUseCase

    @Inject
    lateinit var taskRepo: IDownloadTaskRepository

    private val uiHandler: Handler by lazy {
        Handler(mainLooper)
    }

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

    //下载任务状态数据 url to TaskStatus
    private val downloadingTaskStatusData: MutableMap<String, TaskStatus> = mutableMapOf()


    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            if (application == null)
                return@launch
            val app = application as StationDownloaderApp
            if (!app.isInitialized()) {
                app.initialize()
            }
            if (!engineRepoUseCase.isEnginesInitialized()) {
                engineRepoUseCase.initEngines { engine, result ->
                    if (engine == DownloadEngine.ARIA2) {
                        if (result.succeeded) {
                            engineRepo.setAria2NotifyListener(this@TaskService)
                        }
                    }
                }
            } else {
                if (engineRepo.isEngineInitialized(DownloadEngine.ARIA2)) {
                    engineRepo.setAria2NotifyListener(this@TaskService)
                }
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
                    downloadingTaskStatusData.remove(url)
                    watchTaskJobMap.remove(url)?.apply { cancel() }
                }

                ACTION_CANCEL_ALL_ARIA2_WATCH_TASK -> {
                    watchTaskJobMap.filter {
                        downloadingTaskStatusData[it.key]?.taskId?.engine == DownloadEngine.ARIA2
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
        watchTaskJobMap.forEach { watchTaskJobMap.remove(it.key)?.cancel() }
        startTaskJobMap.forEach { startTaskJobMap.remove(it.key)?.cancel() }
        stopTaskJobMap.forEach { stopTaskJobMap.remove(it.key)?.cancel() }
        serviceScope.cancel()
    }


    fun startTask(url: String, callback: ITaskServiceCallback? = null) =
        serviceScope.launch(Dispatchers.Default) {
            try {
                val xlEntity = taskRepo.getTaskByUrl(url) ?: return@launch
                if (downloadingTaskStatusData[url] != null) {
                    stopTaskJobMap.remove(url)?.apply { cancel() }
                    stopTaskJobMap[url] = stopTask(url)
                    stopTaskJobMap[url]?.join()
                }
                val downloadTask = xlEntity.asStationDownloadTask()
                val taskIdResult = engineRepo.startTask(downloadTask)
                if (taskIdResult is IResult.Error) {
                    Logger.e("${taskIdResult.exception}")
                    sendLocalBroadcast(Intent(
                        ACTION_START_TASK_RESULT
                    ).apply {
                        putExtra("url", url)
                        putExtra("result", false)
                        putExtra("reason", taskIdResult.exception.message.toString())
                    })
                    updateTaskStatusNow(
                        url,
                        TaskId(downloadTask.engine, INVALID_ID),
                        ITaskState.ERROR.code
                    )
                    callback?.onFailed(
                        /* reason = */ taskIdResult.exception.message.toString(),
                        /* code = */ TaskExecuteError.START_TASK_FAILED.ordinal
                    ) ?: sendErrorToClient(
                        command = "start_task",
                        reason = taskIdResult.exception.message.toString(),
                        code = TaskExecuteError.START_TASK_FAILED.ordinal
                    )
                    return@launch
                }
                val taskId = (taskIdResult as IResult.Success).data
//                val status = TaskStatus(
//                    taskId = taskId,
//                    url = url,
//                    speed = 0,
//                    downloadSize = 0,
//                    totalSize = 0,
//                    status = ITaskState.UNKNOWN.code
//                )
//                downloadingTaskStatusData[url] = status//FIXME 为什么要先添加到map中，再更新状态？
                updateTaskStatusNow(url, taskId, ITaskState.RUNNING.code)
                val remoteStartTask = RemoteStartTask.Create(xlEntity, taskId.id, torrentRepo)
                callback?.onResult(MoshiHelper.toJson(remoteStartTask))
                    ?: sendToClient(
                        command = "start_task",
                        data = MoshiHelper.toJson(remoteStartTask),
                        expendJson = null
                    )
                watchTaskJobMap[url]?.apply { cancel() }
                watchTaskJobMap[url] = createWatchTask(taskId, url)
            } catch (e: Exception) {
                logError(e.message.toString())
                callback?.onFailed(
                    e.message.toString(),
                    TaskExecuteError.START_TASK_FAILED.ordinal
                ) ?: sendErrorToClient(
                    command = "start_task",
                    reason = e.message.toString(),
                    code = TaskExecuteError.START_TASK_FAILED.ordinal
                )
            } finally {
                startTaskJobMap.remove(url)
            }
        }


    fun stopTask(url: String, callback: ITaskServiceCallback? = null) =
        serviceScope.launch(Dispatchers.Default) {
            try {
                watchTaskJobMap.remove(url)?.apply { cancel() }
                val entity = taskRepo.getTaskByUrl(url)
                if (entity == null) {
                    callback?.onFailed(
                        "Download task not found!",
                        TaskExecuteError.STOP_TASK_FAILED.ordinal
                    ) ?: sendErrorToClient(
                        command = "stop_task",
                        reason = "Download task not found!",
                        code = TaskExecuteError.STOP_TASK_FAILED.ordinal
                    )
                    return@launch
                }
                val taskId = downloadingTaskStatusData[url]?.taskId
                if (taskId == null || taskId.isInvalid()) {
                    taskRepo.updateTask(entity.copy(status = DownloadTaskStatus.PAUSE))
                } else {
                    engineRepo.stopTask(taskId, entity.asStationDownloadTask())
                    callback?.onResult(MoshiHelper.toJson(RemoteStopTask(url)))
                        ?: sendToClient(
                            command = "stop_task",
                            data = MoshiHelper.toJson(RemoteStopTask(url)),
                            expendJson = null
                        )
                }
                taskId?.let { updateTaskStatusNow(url, it, ITaskState.STOP.code) }
            } catch (e: Exception) {
                logError(e.message.toString())
                callback?.onFailed(
                    e.message.toString(),
                    TaskExecuteError.STOP_TASK_FAILED.ordinal
                ) ?: sendErrorToClient(
                    command = "stop_task",
                    reason = e.message.toString(),
                    code = TaskExecuteError.STOP_TASK_FAILED.ordinal
                )
            } finally {
                stopTaskJobMap.remove(url)
                downloadingTaskStatusData.remove(url)
                if (downloadingTaskStatusData[url] == null && watchTaskJobMap[url] == null && stopTaskJobMap[url] == null) {
                    logger("already stop task $url")
                }
            }

        }

    fun deleteTask(
        url: String,
        isDeleteFile: Boolean,
        callback: ITaskServiceCallback? = null
    ) = serviceScope.launch(Dispatchers.Default) {
        val entity = taskRepo.getTaskByUrl(url)
        if (entity == null) {
            callback?.onFailed(
                "Task not found!",
                TaskExecuteError.TASK_NOT_FOUND.ordinal
            ) ?: sendErrorToClient(
                command = if (isDeleteFile) "remove_task_and_file" else "remove_task",
                reason = "Task not found!",
                code = TaskExecuteError.TASK_NOT_FOUND.ordinal
            )
            sendLocalBroadcast(
                Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                    .putExtra("result", false)
            )
            return@launch
        }
        //FIXME 优化不同引擎的删除任务逻辑。XL引擎删除任务时，会自动停止任务，所以这里先停止任务，再删除任务。ARI2引擎删除任务时，不会自动停止任务，所以这里不需要停止任务，直接删除任务即可。
        if (entity.engine == DownloadEngine.XL) {
            stopTask(url)
        }
        if (entity.engine == DownloadEngine.ARIA2) {
            engineRepo.removeAria2Task(entity.realUrl)
        }
        val deleteResult = taskRepo.deleteTask(url, isDeleteFile)
        if (deleteResult is IResult.Error) {
            Logger.e(deleteResult.exception.message.toString())
            callback?.onFailed(
                "delete task 【$url】failed",
                TaskExecuteError.DELETE_TASK_FAILED.ordinal
            ) ?: sendErrorToClient(
                command = if (isDeleteFile) "remove_task_and_file" else "remove_task",
                reason = "delete task 【$url】failed",
                code = TaskExecuteError.DELETE_TASK_FAILED.ordinal
            )
            sendLocalBroadcast(
                Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                    .putExtra("result", false)
            )
        } else {
            deleteResult as IResult.Success
            callback?.onResult(MoshiHelper.toJson(RemoteDeleteTask(url)))
                ?: sendToClient(
                    command = if (isDeleteFile) "remove_task_and_file" else "remove_task",
                    data = MoshiHelper.toJson(RemoteDeleteTask(url)),
                    expendJson = null
                )
            sendLocalBroadcast(
                Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                    .putExtra("result", true)
            )
        }
    }


    //FIXME 默认下载方式的speedTest需要分离出来，watchTask仅负责监听任务状态，不负责speedTest
    private fun createWatchTask(taskId: TaskId, url: String): Job {
        return serviceScope.launch {
            try {
                when (taskId.engine) {
                    DownloadEngine.XL -> speedTest(taskId, url)
                    DownloadEngine.ARIA2 -> aria2TellStatus(taskId, url)
                    DownloadEngine.INVALID_ENGINE -> TODO()
                }
            } finally {
                logger("watchTask ${taskId.engine}[${taskId.id}] END")
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
                    sendToClient(
                        "download_status",
                        MoshiHelper.toJson(
                            RemoteTaskStatus(
                                download_size = taskInfo.mDownloadSize,
                                speed = speed,
                                status = ITaskState.DONE.ordinal,
                                url = url,
                                is_done = true,
                                total_size = taskInfo.mFileSize,
                                task_id = taskId.id
                            )
                        ),
                        null
                    )
                }
                downloadingTaskStatusData.remove(url)
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
                updateTaskStatus(
                    url,
                    TaskStatus(
                        taskId = taskId,
                        url = url,
                        speed = 0,
                        downloadSize = 0,
                        totalSize = 0,
                        status = ITaskState.ERROR.code,
                    )
                )
                return
            }
            val taskStatus = (taskStatusResult as IResult.Success).data
            updateTaskStatus(url, taskStatus)
            taskRepo.updateTaskStatus(
                url,
                taskStatus.downloadSize,
                taskStatus.totalSize,
                when (taskStatus.status) {
                    ITaskState.RUNNING.code -> DownloadTaskStatus.DOWNLOADING
                    ITaskState.STOP.code -> DownloadTaskStatus.PAUSE
                    ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
                    else -> DownloadTaskStatus.FAILED
                }
            )
            logger("updateTaskStatus ${taskStatus.status}")
            if (taskStatus.status == ITaskState.STOP.code) {
                downloadingTaskStatusData.remove(url)
                watchTaskJobMap.remove(url)?.cancel()
                return
            }
            delay(1000)
        }
    }

    private var shouldStop = false

    private fun updateTaskStatusNow(url: String, taskId: TaskId, status: Int) {
        when (taskId.engine) {
            DownloadEngine.XL -> {
                if (taskId.isInvalid()) {
                    logger("$taskId isInvaild")

                    val status = TaskStatus(taskId = taskId, status = status, url = url)
                    logger("updateTaskStatusNow $status")

                    updateTaskStatus(url, status)
                    return
                }
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
                if (status == ITaskState.ERROR.code) {
                    serviceScope.launch {
                        val statusResponse = engineRepo.getAria2TaskStatusByUrl(url)
                        if (statusResponse.succeeded) {
                            updateTaskStatus(url, statusResponse.result())
                        } else {
                            logLine((statusResponse as IResult.Error).exception)
                            updateTaskStatus(
                                url,
                                TaskStatus(
                                    taskId = taskId,
                                    url = url,
                                    status = ITaskState.ERROR.code
                                )
                            )
                        }
                    }
                    return
                }
                updateTaskStatus(url, TaskStatus(taskId = taskId, url = url, status = status))
            }

            DownloadEngine.INVALID_ENGINE -> TODO()
        }

    }

    private fun updateTaskStatus(url: String, status: TaskStatus) {
        downloadingTaskStatusData[url] = status
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

    fun getDownloadingTaskStatusMap(): MutableMap<String, TaskStatus> {
        return downloadingTaskStatusData
    }

    private fun sendLocalBroadcast(intent: Intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendToClient(command: String, data: String, expendJson: String?) {
        taskStatusBinder?.sendToClient(command, data, expendJson)
    }

    private fun sendErrorToClient(command: String, reason: String, code: Int) {
        taskStatusBinder?.sendErrorToClient(command, reason, code)
    }

    override fun DLogger.tag(): String {
        return TaskService.javaClass.simpleName
    }

    override suspend fun onDownloadStart(gid: String) {
    }

    override suspend fun onDownloadPause(gid: String) {
    }

    override suspend fun onDownloadStop(gid: String) {
    }

    override suspend fun onDownloadComplete(gid: String) {
        logger("onDownloadComplete 下载完成 [${gid}]")
        try {
            val iterator = downloadingTaskStatusData.iterator()
            while (iterator.hasNext()) {
                val (url, taskStatus) = iterator.next()
                if (taskStatus.taskId.id == gid) {
                    stopTask(url)
                    //FIXME 处理Aria2下载完成的流程
                    val statusResponse = engineRepo.getAria2TaskStatus(gid, url)
                    if (statusResponse.succeeded) {
                        val newTaskStatus = statusResponse.result()
                        taskRepo.updateTaskStatus(
                            url,
                            newTaskStatus.downloadSize,
                            newTaskStatus.totalSize,
                            when (newTaskStatus.status) {
                                ITaskState.RUNNING.code -> DownloadTaskStatus.DOWNLOADING
                                ITaskState.STOP.code -> DownloadTaskStatus.PAUSE
                                ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
                                else -> DownloadTaskStatus.FAILED
                            }
                        )
                        _taskStatusFlow.update { newTaskStatus }
                        iterator.remove()
                        watchTaskJobMap.remove(url)?.cancel()
                        sendToClient(
                            "download_status",
                            MoshiHelper.toJson(
                                RemoteTaskStatus(
                                    download_size = newTaskStatus.downloadSize,
                                    speed = newTaskStatus.speed,
                                    status = ITaskState.DONE.ordinal,
                                    url = url,
                                    is_done = true,
                                    total_size = newTaskStatus.totalSize,
                                    task_id = gid
                                )
                            ),
                            null
                        )
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Logger.e("${e.message}")
        }
    }

    override suspend fun onDownloadError(gid: String) {
    }

    override suspend fun onBtDownloadComplete(gid: String) {
    }

    fun showToastOnUIThread(msg: String) {
        uiHandler.post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
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

