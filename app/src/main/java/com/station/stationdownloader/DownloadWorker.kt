package com.station.stationdownloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.station.stationdownloader.data.source.IEngineRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class DownloadWorker(
    context: Context, workerParameters: WorkerParameters,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private var mWorkerEntryPoint: DownloadWorkerEntryPoint
    private var mEngineRepo: IEngineRepository

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DownloadWorkerEntryPoint {
        fun getEngineRepo(): IEngineRepository
    }

    init {
        mWorkerEntryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadWorkerEntryPoint::class.java
        )
        mEngineRepo = mWorkerEntryPoint.getEngineRepo()
    }

    override suspend fun doWork(): Result {
        val url: String = inputData.getString("url") ?: return Result.failure()
        val engine: String = inputData.getString("engine") ?: return Result.failure()
        val downloadPath: String = inputData.getString("downloadPath") ?: return Result.failure()
        val name: String = inputData.getString("name") ?: return Result.failure()
        val urlType: String = inputData.getString("urlType") ?: return Result.failure()
        val fileCount: Int = inputData.getInt("fileCount", 0) ?: return Result.failure()
        val selectIndexes: IntArray =
            inputData.getIntArray("selectIndexes") ?: emptyList<Int>().toIntArray()


        val startTaskResult = mEngineRepo.startTask(
            url = url,
            engine = DownloadEngine.valueOf(engine),
            downloadPath = downloadPath,
            name = name,
            urlType = DownloadUrlType.valueOf(urlType),
            fileCount = fileCount,
            selectIndexes = selectIndexes
        )

        return Result.success()
    }

    companion object {
        const val KEY_URL = "url"
        const val ENGINE = "engine"
        const val DOWNLOAD_PATH = "download_path"
        const val NAME = "name"
        const val URL_TYPE = ""
        const val SELECT_INDEXES = ""

    }
}