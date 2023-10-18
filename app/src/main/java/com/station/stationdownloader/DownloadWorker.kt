package com.station.stationdownloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.orhanobut.logger.Logger
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

class DownloadWorker(
    context: Context, workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters), DLogger {
    private var mWorkerEntryPoint: DownloadWorkerEntryPoint
    private var mEngineRepo: DefaultEngineRepository
    private var mDownloadTaskRepo: IDownloadTaskRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadWorkerEntryPoint {
        fun getEngineRepo(): DefaultEngineRepository

        fun getDownloadTaskRepo(): IDownloadTaskRepository
    }

    init {
        Logger.d("init worker")
        mWorkerEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java
        )
        mEngineRepo = mWorkerEntryPoint.getEngineRepo()
        mDownloadTaskRepo = mWorkerEntryPoint.getDownloadTaskRepo()
    }

    override suspend fun doWork(): Result {
        mEngineRepo.init()
        Logger.d("doWork()==>start")

        val errorReason = workDataOf(
            FAILURE_REASON to TaskExecuteError.NOT_ENOUGH_WORKER_INPUT_ARGUMENTS.name
        )
        logger("获取输入数据")
        val url: String = inputData.getString(IN_URL) ?: return Result.failure(errorReason)
        val engine: String = inputData.getString(IN_ENGINE) ?: return Result.failure(errorReason)
        val downloadPath: String =
            inputData.getString(IN_DOWNLOAD_PATH) ?: return Result.failure(errorReason)
        logger("开始从数据库查找任务")
        val getTaskResult =
            mDownloadTaskRepo.getTaskByUrl(url, DownloadEngine.valueOf(engine), downloadPath)

        if (getTaskResult is IResult.Error)
            return Result.failure(workDataOf(FAILURE_REASON to getTaskResult.code))

        val downloadTask = (getTaskResult as IResult.Success).data

        logger("开始启动下载")
        var count=0
        while (true){
            if(count==20)
                return Result.success()
            logger("当前计数：$count")
            count++
            delay(1000)
        }
//        val startTaskResult = mEngineRepo.startTask(downloadTask.asStationDownloadTask())
//
//        if (startTaskResult is IResult.Error) {
//            logger("启动失败")
//            return Result.failure(workDataOf(FAILURE_REASON to startTaskResult.code))
//        }
//
//        var taskId = (startTaskResult as IResult.Success).data
//
//        logger("启动成功！")
//        Thread {
//            while (true) {
//                val taskInfo: XLTaskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
//                logger("获取进度中...")
//                val statusData = workDataOf(
//                    OUT_STATUS to taskInfo.mTaskStatus,
//                    OUT_SPEED to taskInfo.mDownloadSpeed,
//                    OUT_DOWNLOAD_SIZE to taskInfo.mDownloadSize,
//                    OUT_TOTAL_SIZE to taskInfo.mFileSize
//                )
//
//                CoroutineScope(Job()).launch {
//                    logger("返回进度中...")
//
//                    setProgress(statusData)
//                }
//                Thread.sleep(1000)
//            }
//        }.start()
//        while (true) {
//            val taskInfo: XLTaskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
//
//            if (taskInfo.mTaskStatus == ITaskState.DONE.ordinal) {
//                logger("检查下载是否完成...")
//                return Result.success()
//            }
//
//            delay(1000)
//        }


    }


    private fun speedTest(): Boolean {
        return false
    }

    override fun DLogger.tag(): String {
        return DownloadWorker.javaClass.simpleName
    }

    companion object {
        const val WORKER_TAG = "runningTask"

        const val IN_URL = "url"
        const val IN_ENGINE = "engine"
        const val IN_DOWNLOAD_PATH = "download_path"

        const val OUT_STATUS = "status"
        const val OUT_SPEED = "speed"
        const val OUT_DOWNLOAD_SIZE = "download_size"
        const val OUT_TOTAL_SIZE = "total_size"

        const val FAILURE_REASON = "failure_reason"

    }


}