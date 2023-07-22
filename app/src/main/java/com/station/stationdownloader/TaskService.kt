package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.di.IoDispatcher
import com.station.stationdownloader.utils.DLogger
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CloseableCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.internal.concurrent.Task
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class TaskService : Service(), DLogger {
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var engineRepo: IEngineRepository

    @Inject
    lateinit var taskRepo: IDownloadTaskRepository

    private val tasks = mutableMapOf<String, Job>()

    private val taskStatusFlow: MutableStateFlow<Map<String, TaskStatus>> =
        MutableStateFlow(emptyMap())

    override fun onCreate() {
        super.onCreate()
        printCodeLine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (intent.action) {
                ACTION_WATCH_TASK -> {
                    val url = intent.getStringExtra("url") ?: return START_NOT_STICKY
                    val taskId = intent.getLongExtra("taskId", -1)
                    tasks[url]?.apply { cancel() }
                    val job = createWatchTask(taskId, url)
                    tasks[url] = job
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
        tasks.values.forEach { it.cancel() }
        serviceScope.cancel()
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
        if (isStopRetry)
            return

        while (true) {
            Logger.d("speedTest test")
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

            if (delayCount > 0) {
                delayCount--
            } else {
                startSpeedTest = true
            }

            val speed = taskInfo.mDownloadSpeed
            val status = taskInfo.mTaskStatus

            updateTaskStatus(
                url, TaskStatus(
                    taskId = taskId,
                    url = url,
                    speed = taskInfo.mDownloadSpeed,
                    downloadSize = taskInfo.mDownloadSize,
                    totalSize = taskInfo.mFileSize,
                    status = status
                )
            )

            withContext(Dispatchers.IO) {
                val taskStatus = when (status) {
                    ITaskState.RUNNING.code, ITaskState.STOP.code -> DownloadTaskStatus.DOWNLOADING
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
            }

            if (speed <= 0) {
                if (startSpeedTest) noSpeedCount++
            } else {
                delayCount = 0
                retryFailedCount = 0
                noSpeedCount = 0
            }

            if (status == ITaskState.DONE.code) {
                tasks.remove(url)?.cancel()
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

        if (isRestart)
            restartTask(url, retryFailedCount)
    }

    private suspend fun restartTask(url: String, retryCount: Int) {
        val xlTaskEntity = taskRepo.getTaskByUrl(url) ?: return
        val stationDownloadTask = xlTaskEntity.asStationDownloadTask()
        val result = engineRepo.restartTask(stationDownloadTask)
        if (result is IResult.Error) return
        val taskId = (result as IResult.Success).data
        speedTest(taskId, url, retryCount + 1, (retryCount + 1) * 5)
    }

    private fun updateTaskStatus(url: String, status: TaskStatus) {
        taskStatusFlow.value = taskStatusFlow.value.plus(url to status)
    }

    fun getStatusFlow(): StateFlow<Map<String, TaskStatus>> {
        return taskStatusFlow
    }

    fun stopWatchStatus(url: String) {
        tasks.remove(url)?.cancel()
        if (tasks.isEmpty())
            stopSelf()
    }

    override fun DLogger.tag(): String {
        return TaskService.javaClass.simpleName
    }


    // Binder 类用于提供任务状态信息给绑定的组件
    inner class TaskBinder : ITaskStatusService.Stub() {
        fun getService(): TaskService = this@TaskService
        override fun getTaskStatus(): MutableMap<String, TaskStatus> {
            val statusMap = mutableMapOf<String, TaskStatus>()

            // 订阅任务状态的 Flow，并获取任务状态信息
            getStatusFlow().value.entries.forEach { (taskId, status) ->
                statusMap[taskId] = status.copy()
            }
            return statusMap
        }

        override fun getTaskStatusByUrl(url: String?): TaskStatus? {
            return getStatusFlow().value[url]
        }
    }

    companion object {
        private const val ACTION_WATCH_TASK = "action.watch.task"

        @JvmStatic
        fun watchTask(context: Context, url: String, taskId: Long = -1) {
            val intent = Intent(context, TaskService::class.java).setAction(ACTION_WATCH_TASK)
            intent.putExtra("url", url)
            intent.putExtra("taskId", taskId)
            context.startService(intent)
        }
    }
}

