package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.StationTaskInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultEngineRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : IEngineRepository {

    override suspend fun init(): IResult<Unit> {
        return try {
            xlEngine.init()
            aria2Engine.init()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }

    }

    override suspend fun unInit(): IResult<Unit> {
        return try {
            xlEngine.unInit()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }
    }

    override suspend fun initTask(url: String): IResult<NewTaskConfigModel> {
        return xlEngine.initUrl(url)
    }

    override suspend fun startTask(
        url: String,
        engine: DownloadEngine,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<Long> = withContext(Dispatchers.IO) {
        when (engine) {
            DownloadEngine.XL -> {
                val startTaskResult = xlEngine.startTask(
                    url, downloadPath, name, urlType, fileCount, selectIndexes
                )
                startTaskResult
            }

            DownloadEngine.ARIA2 -> {
                val startTaskResult = aria2Engine.startTask(
                    url, downloadPath, name, urlType, fileCount, selectIndexes
                )
                startTaskResult
            }
        }
    }

    override suspend fun getTaskSize(
        startDownloadTask: StationDownloadTask, timeOut: Long
    ): IResult<StationDownloadTask> {
        return xlEngine.getTaskSize(startDownloadTask, timeOut)
    }

    override suspend fun getTaskInfo(taskId: Long): StationTaskInfo {
        return xlEngine.getTaskInfo(taskId)
    }

    override suspend fun configure(key: String, values: Array<String>): IResult<Unit> {
        val xlConfigResult = xlEngine.configure(key, values)
        val aria2ConfigResult = aria2Engine.configure(key, values)

        if (xlConfigResult is IResult.Error) return xlConfigResult.copy(exception = Exception("[xl] ${xlConfigResult.exception.message}"))

        if (aria2ConfigResult is IResult.Error) return aria2ConfigResult.copy(
            exception = Exception(
                "[aria2] ${aria2ConfigResult.exception.message}"
            )
        )

        return IResult.Success(Unit)
    }


}