package com.station.stationdownloader.data.datasource.local

import com.station.stationdownloader.data.datasource.IDownloadTaskDataSource
import com.station.stationdownloader.data.datasource.local.room.dao.DownloadTaskDao
import com.station.stationdownloader.data.datasource.local.room.entities.DownloadTaskEntity
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
class DownloadTaskLocalDataSource(
    private val downloadTaskDao: DownloadTaskDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IDownloadTaskDataSource {
    override suspend fun getTasks(): IResult<List<DownloadTaskEntity>> = withContext(ioDispatcher) {
        return@withContext try {
            IResult.Success(downloadTaskDao.getTasks())
        } catch (e: Exception) {
            IResult.Error(e)
        }
    }

    override  fun getTasksStream(): Flow<IResult<List<DownloadTaskEntity>>> {
            return downloadTaskDao.observeTasksStream().map {
                IResult.Success(it)
            }
    }

    override suspend fun insertTask(task: DownloadTaskEntity): Long {
        return downloadTaskDao.insertTask(task)
    }
}