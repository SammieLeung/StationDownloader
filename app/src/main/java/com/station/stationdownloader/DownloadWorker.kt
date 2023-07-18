package com.station.stationdownloader

import android.content.Context
import androidx.room.Database
import androidx.work.CoroutineWorker
import androidx.work.Operation.State.FAILURE
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.XLTaskInfo
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay

class DownloadWorker(
    context: Context, workerParameters: WorkerParameters,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private var mWorkerEntryPoint: DownloadWorkerEntryPoint
    private var mEngineRepo: IEngineRepository
    private var mDownloadTaskRepo: IDownloadTaskRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadWorkerEntryPoint {
        fun getEngineRepo(): IEngineRepository

        fun getDownloadTaskRepo(): IDownloadTaskRepository
    }

    init {
        mWorkerEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java
        )
        mEngineRepo = mWorkerEntryPoint.getEngineRepo()
        mDownloadTaskRepo = mWorkerEntryPoint.getDownloadTaskRepo()
    }

    override suspend fun doWork(): Result {

        val errorReason = workDataOf(
            FAILURE_REASON to TaskExecuteError.NOT_ENOUGH_WORKER_INPUT_ARGUMENTS
        )
        val url: String = inputData.getString(IN_URL) ?: return Result.failure(errorReason)
        val engine: String = inputData.getString(IN_ENGINE) ?: return Result.failure(errorReason)
        val downloadPath: String =
            inputData.getString(IN_DOWNLOAD_PATH) ?: return Result.failure(errorReason)

        val getTaskResult =
            mDownloadTaskRepo.getTaskByUrl(url, DownloadEngine.valueOf(engine), downloadPath)

        if (getTaskResult is IResult.Error)
            return Result.failure(workDataOf(FAILURE_REASON to getTaskResult.code))

        val downloadTask = (getTaskResult as IResult.Success).data

        val startTaskResult = mEngineRepo.startTask(downloadTask.asStationDownloadTask())

        if (startTaskResult is IResult.Error)
            return Result.failure(workDataOf(FAILURE_REASON to startTaskResult.code))

        var taskId = (startTaskResult as IResult.Success).data

        while (true) {
            val taskInfo: XLTaskInfo = XLTaskHelper.instance().getTaskInfo(taskId)
            val statusData = workDataOf(
                OUT_STATUS to taskInfo.mTaskStatus,
                OUT_SPEED to taskInfo.mDownloadSpeed,
                OUT_DOWNLOAD_SIZE to taskInfo.mDownloadSize,
                OUT_TOTAL_SIZE to taskInfo.mFileSize
            )
            if (taskInfo.mTaskStatus == ITaskState.RUNNING.ordinal) {
                setProgress(statusData)
                if(speedTest()){
                    //重试退避策略 最小时间为10s
                    return Result.retry()
                }

            }

            if (taskInfo.mTaskStatus == ITaskState.DONE.ordinal) {
                setProgress(statusData)
                break
            }

            delay(1000)
        }

        return Result.success()
    }


    private fun speedTest():Boolean{
        return false
    }

    companion object {
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