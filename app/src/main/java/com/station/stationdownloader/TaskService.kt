package com.station.stationdownloader

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

@AndroidEntryPoint
class TaskService : Service() {
    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Inject
    lateinit var engineRepo: IEngineRepository

    @Inject
    lateinit var taskRepo: IDownloadTaskRepository

    val taskMap: MutableMap<String, Long> = mutableMapOf()

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_WATCH_TASK -> {
                    val url = intent.getStringExtra("url")
                    if (url == null) {
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    val taskId = intent.getLongExtra("taskId", -1)
                    val job = serviceScope.launch {
                    }
                }
            }
        }

        stopSelf()
        return START_NOT_STICKY
    }


    class TaskRunnable(
        val taskMap: MutableMap<String, Long>,
        val taskRepo: IDownloadTaskRepository,
        val engineRepo: IEngineRepository,
        var taskId: Long,
        val url: String
    ) : Runnable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var job: Job? = null
        var retryFailedCount = 0

        init {
            taskMap[url] = taskId
        }

        override fun run() {
            job = speedTest(taskId)
        }

        private fun speedTest(taskId: Long, retryFailedCount: Int = 0, delayTestTime: Int = 30) =
            scope.launch {
                var delayCount = delayTestTime
                var nullCount = 0
                var startSpeedTest = false
                var retryCount = retryFailedCount
                var noSpeedCount = 0

                while (isActive) {
                    val taskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
                    if (taskInfo == null) {
                        if (nullCount < 5) {
                            nullCount++
                            delay(1000)
                            continue
                        }
                        cancel(CancellationException("taskInfo is null"))
                    }
                    nullCount = 0

                    if (delayCount > 0) {
                        delayCount--
                    }else{
                        startSpeedTest = true
                    }

                    val speed = taskInfo.mDownloadSpeed

                    if (speed <= 0) {
                        if (startSpeedTest)
                            noSpeedCount++
                    } else {
                        delayCount=0
                        retryCount=0
                        noSpeedCount = 0
                    }

                    if (noSpeedCount > 5) {
                        if (retryCount > 10) {
                            cancel(CancellationException("try failed too much"))
                        }
                        restartTask(retryCount)
                        cancel()
                    }

                    delay(1000)
                }
            }

        private suspend fun restartTask(retryCount: Int) {
            val xlTaskEntity = taskRepo.getTaskByUrl(url) ?: return
            val stationDownloadTask = xlTaskEntity.asStationDownloadTask()
            val result = engineRepo.restartTask(stationDownloadTask)
            if (result is IResult.Error)
                return
            val taskId = (result as IResult.Success).data
            job = speedTest(taskId, retryCount + 1)
        }

    }


    private fun watchStatus(url: String, taskId: Long) {
        var noSpeedCount = 0
        var resetCount = 0
        var needReset = false


        val job = serviceScope.launch {
            isActive
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


    companion object {
        private const val ACTION_START_SERVICE = "start.service"
        private const val ACTION_STOP_SERVICE = "stop.service"
        private const val ACTION_WATCH_TASK = "action.watch.task"

        @JvmStatic
        fun startService(context: Context) {
            context.startService(
                Intent(context, TaskService::class.java).setAction(
                    ACTION_START_SERVICE
                )
            )
        }

        @JvmStatic
        fun stopService(context: Context) {
            context.startService(
                Intent(context, TaskService::class.java)
                    .setAction(ACTION_STOP_SERVICE)
            )
        }


        @JvmStatic
        fun watchTask(context: Context, url: String, taskId: Long?) {

        }
    }
}