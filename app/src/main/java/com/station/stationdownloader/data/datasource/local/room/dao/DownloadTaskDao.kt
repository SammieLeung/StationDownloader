package com.station.stationdownloader.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.station.stationdownloader.data.datasource.local.room.entities.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_task")
    suspend fun getTasks(): List<DownloadTaskEntity>
    @Query("SELECT * FROM download_task")
    fun observeTasksStream(): Flow<List<DownloadTaskEntity>>
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: DownloadTaskEntity):Long
    @Delete
    suspend fun deleteTask(task: DownloadTaskEntity):Int

}