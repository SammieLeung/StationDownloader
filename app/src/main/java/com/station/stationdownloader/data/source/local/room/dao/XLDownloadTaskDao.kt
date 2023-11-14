package com.station.stationdownloader.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
@Dao
interface XLDownloadTaskDao {
    @Query("SELECT * FROM xl_download_task")
    suspend fun getTasks(): List<XLDownloadTaskEntity>
    @Query("SELECT * FROM xl_download_task WHERE url=:url")
    suspend fun getTaskByUrl(url:String):XLDownloadTaskEntity?

    @Query("SELECT * FROM xl_download_task WHERE real_url=:realUrl")
    suspend fun getTaskByRealUrl(realUrl:String):XLDownloadTaskEntity?

    @Query("SELECT * FROM xl_download_task WHERE url=:url AND download_path=:downloadPath")
    suspend fun getTaskByUrl(url:String,downloadPath:String):XLDownloadTaskEntity?

    @Query("SELECT * FROM xl_download_task WHERE torrent_id=:torrentId")
    suspend fun getTaskByTorrentId(torrentId:Long):XLDownloadTaskEntity?

    @Query("SELECT * FROM xl_download_task")
    fun observeTasksStream(): Flow<List<XLDownloadTaskEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: XLDownloadTaskEntity):Long
    @Query("DELETE FROM xl_download_task WHERE url=:url")
    suspend fun deleteTask(url:String):Int
    @Update
    suspend fun updateTask(task: XLDownloadTaskEntity):Int

    @Query("UPDATE xl_download_task SET status=:status,download_size=:downloadSize,total_size=:totalSize WHERE url=:url")
    suspend fun updateTaskStatus(url:String, downloadSize:Long, totalSize:Long, status:DownloadTaskStatus):Int
}