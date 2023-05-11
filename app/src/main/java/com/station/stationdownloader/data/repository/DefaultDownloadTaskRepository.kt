package com.station.stationdownloader.data.repository

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.IDownloadTaskDataSource
import com.station.stationdownloader.data.datasource.IDownloadTaskRepository
import com.station.stationdownloader.data.datasource.local.room.entities.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
class DefaultDownloadTaskRepository(
    private val localDataSource: IDownloadTaskDataSource,
) : IDownloadTaskRepository {
    override suspend fun getTasks(): List<DownloadTaskEntity> {
        return when (val result = localDataSource.getTasks()) {
            is IResult.Success -> {
                result.data
            }

            is IResult.Error -> {
                //TODO 处理错误
                emptyList()
            }
        }

    }

    override fun getTasksStream(): Flow<IResult<List<DownloadTaskEntity>>> {
        return localDataSource.getTasksStream()
    }

    override suspend fun insertTask(task: DownloadTaskEntity): Long {
        return localDataSource.insertTask(task)
    }
}