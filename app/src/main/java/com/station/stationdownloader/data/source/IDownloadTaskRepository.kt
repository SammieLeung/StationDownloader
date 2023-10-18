package com.station.stationdownloader.data.source

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
interface IDownloadTaskRepository {
    suspend fun getTasks(): List<XLDownloadTaskEntity>
    suspend fun getTaskByUrl(url: String): XLDownloadTaskEntity?
    suspend fun getTaskByUrl(
        url: String,
        engine: DownloadEngine,
        downloadPath: String
    ): IResult<XLDownloadTaskEntity>
    suspend fun getTorrentTaskByHash(infoHash:String):XLDownloadTaskEntity?

    fun getTasksStream(): Flow<IResult<List<XLDownloadTaskEntity>>>
    suspend fun insertTask(task: XLDownloadTaskEntity): IResult<Long>
    suspend fun updateTask(task: XLDownloadTaskEntity): IResult<Int>
    suspend fun deleteTask(url: String, deleteFile: Boolean): IResult<Int>
    suspend fun saveTask(newTask: NewTaskConfigModel): IResult<XLDownloadTaskEntity>
    suspend fun saveTask(
        torrentId: Long = -1,
        originUrl: String,
        realUrl: String,
        taskName: String,
        urlType: DownloadUrlType,
        engine: DownloadEngine,
        totalSize: Long,
        downloadPath: String,
        selectIndexes: List<Int>,
        fileList: List<String> = emptyList(),
        fileCount: Int
    ): IResult<XLDownloadTaskEntity>
}