package com.station.stationdownloader.data.source.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity

@Dao
interface TorrentInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTorrentInfo(torrentInfo: TorrentInfoEntity):Long

    @Update
    suspend fun updateTorrentInfo(torrentInfo: TorrentInfoEntity):Int

    @Query("SELECT * FROM torrent_info AS TI " +
            "JOIN torrent_file_info AS TFI ON TFI.torrent_id = TI.id ")
    suspend fun getTorrentInfo():Map<TorrentInfoEntity,List<TorrentFileInfoEntity>>

    @Query("SELECT * FROM torrent_info AS TI " +
            "JOIN torrent_file_info AS TFI ON TFI.torrent_id = TI.id WHERE hash=:hash")
    suspend fun getTorrentByHash(hash:String):Map<TorrentInfoEntity,List<TorrentFileInfoEntity>>

    @Query("SELECT id FROM torrent_info " +
            "WHERE hash=:hash ")
    suspend fun getTorrentId(hash:String):Long

}