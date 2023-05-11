package com.station.stationdownloader.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity

@Dao
interface TorrentFileInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity)

    @Update
    suspend fun updateTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity)


}