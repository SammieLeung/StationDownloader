package com.station.stationdownloader.data.source.local

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.contants.TaskExecuteError
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
    override suspend fun getTasks(): IResult<List<XLDownloadTaskEntity>> =
        withContext(ioDispatcher) {
            return@withContext try {
                IResult.Success(downloadTaskDao.getTasks())
            } catch (e: Exception) {
                IResult.Error(exception = e)
            }
        }

    override fun getTasksStream(): Flow<IResult<List<XLDownloadTaskEntity>>> {
        return downloadTaskDao.observeTasksStream().map {
            IResult.Success(it)
        }
    }

    override suspend fun insertTask(task: XLDownloadTaskEntity): IResult<Long> =
        withContext(ioDispatcher) {
            val id = downloadTaskDao.insertTask(task)
            if (id > 0) {
                IResult.Success(id)
            } else {
                IResult.Error(
                    Exception(TaskExecuteError.TASK_INSERT_ERROR.name),
                    TaskExecuteError.TASK_INSERT_ERROR.ordinal
                )
            }
        }

    override suspend fun getTaskByUrl(url: String): XLDownloadTaskEntity? =
        withContext(ioDispatcher) {
            downloadTaskDao.getTaskByUrl(url)
        }

    override suspend fun getTaskByUrl(
        url: String,
        downloadPath: String
    ): IResult<XLDownloadTaskEntity> = withContext(ioDispatcher) {
        val xlDownloadTaskEntity = downloadTaskDao.getTaskByUrl(url, downloadPath)
        return@withContext if (xlDownloadTaskEntity == null) {
            IResult.Error(
                Exception(TaskExecuteError.TASK_NOT_FOUND.name),
                TaskExecuteError.TASK_NOT_FOUND.ordinal
            )
        } else {
            IResult.Success(xlDownloadTaskEntity)
        }
    }

    override suspend fun getTaskByTorrentId(torrentId: Long): XLDownloadTaskEntity? {
        return downloadTaskDao.getTaskByTorrentId(torrentId)
    }

    override suspend fun updateTask(task: XLDownloadTaskEntity): IResult<Int> =
        withContext(ioDispatcher) {
            try {
                IResult.Success(downloadTaskDao.updateTask(task))
            } catch (e: Exception) {
                IResult.Error(exception = e)
            }
        }

    override suspend fun deleteTask(url: String): IResult<Int> = withContext(ioDispatcher) {
        try {
            IResult.Success(downloadTaskDao.deleteTask(url))
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }
    }
}