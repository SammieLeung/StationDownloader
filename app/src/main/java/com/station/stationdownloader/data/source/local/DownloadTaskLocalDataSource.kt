package com.station.stationdownloader.data.source.local

import com.station.stationdownloader.data.source.IDownloadTaskDataSource
import com.station.stationdownloader.data.source.local.room.dao.XLDownloadTaskDao
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import kotlinx.coroutines.CoroutineDispatcher
import com.station.stationdownloader.data.IResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
class DownloadTaskLocalDataSource internal constructor(
    private val downloadTaskDao: XLDownloadTaskDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IDownloadTaskDataSource {
    override suspend fun getTasks(): IResult<List<XLDownloadTaskEntity>> = withContext(ioDispatcher) {
        return@withContext try {
            IResult.Success(downloadTaskDao.getTasks())
        } catch (e: Exception) {
            IResult.Error(exception =  e)
        }
    }

    override  fun getTasksStream(): Flow<IResult<List<XLDownloadTaskEntity>>> {
            return downloadTaskDao.observeTasksStream().map {
                IResult.Success(it)
            }
    }

    override suspend fun insertTask(task: XLDownloadTaskEntity): Long {
        return downloadTaskDao.insertTask(task)
    }

    override suspend fun getTaskByUrl(url: String): XLDownloadTaskEntity? {
        return downloadTaskDao.getTaskByUrl(url)
    }
}