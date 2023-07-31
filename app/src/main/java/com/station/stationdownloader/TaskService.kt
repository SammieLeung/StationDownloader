package com.station.stationdownloader

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.di.AppCoroutineScope
import com.station.stationdownloader.di.IoDispatcher
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.wait
import java.io.File
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

    private val watchTaskJobMap = mutableMapOf<String, Job>()
    private var startTaskJobMap = mutableMapOf<String, Job>()
    private var stopTaskJobMap = mutableMapOf<String, Job>()

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
                    cancelJob(url)
                    startTaskJobMap[url] = startTask(url)
                }

                ACTION_STOP_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    cancelJob(url)
                    stopTaskJobMap[url] = stopTask(url)
                }

                ACTION_DELETE_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    val isDeleteFile = intent.getBooleanExtra("isDeleteFile",false)
                    serviceScope.launch {
                        val job = deleteTask(url,isDeleteFile).await()
                        if (job is IResult.Error) {
                            Logger.e(job.exception.message.toString())
                            LocalBroadcastManager.getInstance(this@TaskService)
                                .sendBroadcast(
                                    Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                                        .putExtra("result", false)
                                )
                        } else {
                            job as IResult.Success
                            LocalBroadcastManager.getInstance(this@TaskService)
                                .sendBroadcast(
                                    Intent(ACTION_DELETE_TASK_RESULT).putExtra("url", url)
                                        .putExtra("result", true)
                                )
                        }

                    }


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

            val xlEntity = taskRepo.getTaskByUrl(url) ?: return@launch
            val taskIdResult = engineRepo.startTask(xlEntity.asStationDownloadTask())
            if (taskIdResult is IResult.Error) return@launch
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
        } catch (e: Exception) {
            logError(e.message.toString())
        }
    }

    private fun stopTask(url: String) = serviceScope.launch {
        try {
            watchTaskJobMap.remove(url)?.apply { cancel() }
            val entity = taskRepo.getTaskByUrl(url) ?: return@launch
            val taskId = runningTaskMap.remove(url)?.taskId
            if (taskId == null) {
                taskRepo.updateTask(entity.copy(status = DownloadTaskStatus.PAUSE))
            } else {
                engineRepo.stopTask(taskId, entity.asStationDownloadTask())
            }
            updateTaskStatusNow(url, -1, ITaskState.STOP.code)
        } catch (e: Exception) {
            logError(e.message.toString())
        }

    }

    private fun deleteTask(url: String,isDeleteFile: Boolean): Deferred<IResult<Int>> = serviceScope.async<IResult<Int>> {
        stopTask(url)
        taskRepo.deleteTask(url,isDeleteFile)
    }

    private fun cancelJob(url: String) {
        startTaskJobMap[url]?.cancel()
        stopTaskJobMap[url]?.cancel()
    }

    private fun createWatchTask(taskId: Long, url: String): Job {
        return serviceScope.launch {
            speedTest(taskId, url)
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
        fun deleteTask(context: Context, url: String,isDeleteFile:Boolean) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_DELETE_TASK)
            intent.putExtra("url", url)
            intent.putExtra("isDeleteFile",isDeleteFile)
            context.startService(intent)
        }
    }
}

