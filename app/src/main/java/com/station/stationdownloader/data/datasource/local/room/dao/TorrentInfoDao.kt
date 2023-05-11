package com.station.stationdownloader.data.datasource.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentInfo

@Dao
interface TorrentInfoDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTorrentInfo(torrentInfo: TorrentInfoEntity)

    @Update
    suspend fun updateTorrentInfo(torrentInfo: TorrentInfoEntity)

    @Query("SELECT * FROM torrent_info AS TI " +
            "JOIN torrent_file_info AS TFI ON TFI.torrent_id = TI.id ")
    suspend fun getTorrentInfo():Map<TorrentInfoEntity,List<TorrentFileInfoEntity>>

}