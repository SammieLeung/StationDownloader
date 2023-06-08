package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.local.room.entities.XLDownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
interface IDownloadTaskDataSource {
   suspend fun getTasks(): IResult<List<XLDownloadTaskEntity>>
   fun getTasksStream(): Flow<IResult<List<XLDownloadTaskEntity>>>
   suspend fun insertTask(task: XLDownloadTaskEntity):Long
}