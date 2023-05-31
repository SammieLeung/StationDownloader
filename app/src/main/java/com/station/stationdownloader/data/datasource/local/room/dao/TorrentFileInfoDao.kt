package com.station.stationdownloader.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.xunlei.downloadlib.parameter.TorrentFileInfo

@Dao
interface TorrentFileInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity):Long

    @Update
    suspend fun updateTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity):Int

    @Query("SELECT * FROM torrent_file_info " +
            "WHERE torrent_id=:torrentId AND real_index=:realIndex")
    suspend fun getTorrentFileInfo(torrentId:Long,realIndex:Int):TorrentFileInfoEntity?
}