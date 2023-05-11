package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.local.room.entities.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
interface IDownloadTaskRepository {
    suspend fun getTasks():List<DownloadTaskEntity>
    fun getTasksStream(): Flow<IResult<List<DownloadTaskEntity>>>
    suspend fun insertTask(task: DownloadTaskEntity):Long
}