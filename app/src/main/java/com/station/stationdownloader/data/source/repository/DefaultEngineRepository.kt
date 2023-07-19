package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.asXLDownloadTaskEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultEngineRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val downloadTaskRepo: IDownloadTaskRepository,
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

    override suspend fun initUrl(url: String): IResult<NewTaskConfigModel> {
        return xlEngine.initUrl(url)
    }

    override suspend fun startTask(stationDownloadTask: StationDownloadTask): IResult<Long> =
        withContext(Dispatchers.IO) {
            val url: String = stationDownloadTask.realUrl
            val downloadPath: String = stationDownloadTask.downloadPath
            val name: String = stationDownloadTask.name
            val urlType: DownloadUrlType = stationDownloadTask.urlType
            val fileCount: Int = stationDownloadTask.fileCount
            val selectIndexes: IntArray = stationDownloadTask.selectIndexes.toIntArray()
            return@withContext when (stationDownloadTask.engine) {
                DownloadEngine.XL -> {
                    val startTaskResult = xlEngine.startTask(
                        url, downloadPath, name, urlType, fileCount, selectIndexes
                    )
                    downloadTaskRepo.updateTask(stationDownloadTask.copy(status = DownloadTaskStatus.DOWNLOADING).asXLDownloadTaskEntity())
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