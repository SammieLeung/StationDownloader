package com.station.stationdownloader.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
    @Query("SELECT * FROM xl_download_task")
    fun observeTasksStream(): Flow<List<XLDownloadTaskEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: XLDownloadTaskEntity):Long
    @Delete
    suspend fun deleteTask(task: XLDownloadTaskEntity):Int

}