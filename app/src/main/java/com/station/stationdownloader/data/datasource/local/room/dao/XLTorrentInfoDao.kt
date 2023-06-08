package com.station.stationdownloader.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.station.stationdownloader.data.datasource.local.room.entities.XLTorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.XLTorrentInfoEntity

@Dao
interface XLTorrentInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTorrentInfo(torrentInfo: XLTorrentInfoEntity):Long

    @Update
    suspend fun updateTorrentInfo(torrentInfo: XLTorrentInfoEntity):Int

    @Query("SELECT * FROM xl_torrent_info AS TI " +
            "JOIN xl_torrent_file_info AS TFI ON TFI.torrent_id = TI.id ")
    suspend fun getTorrentInfo():Map<XLTorrentInfoEntity,List<XLTorrentFileInfoEntity>>

    @Query("SELECT id FROM xl_torrent_info " +
            "WHERE hash=:hash ")
    suspend fun getTorrentId(hash:String):Long

}