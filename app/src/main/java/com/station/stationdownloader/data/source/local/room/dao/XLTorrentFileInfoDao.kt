package com.station.stationdownloader.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentFileInfoEntity

@Dao
interface XLTorrentFileInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTorrentFileInfo(torrentFileInfo: XLTorrentFileInfoEntity):Long

    @Update
    suspend fun updateTorrentFileInfo(torrentFileInfo: XLTorrentFileInfoEntity):Int

    @Query("SELECT * FROM xl_torrent_file_info " +
            "WHERE torrent_id=:torrentId AND real_index=:realIndex")
    suspend fun getTorrentFileInfo(torrentId:Long,realIndex:Int):XLTorrentFileInfoEntity?
}