package com.station.stationdownloader.data.datasource.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.station.stationdownloader.data.datasource.local.room.converter.StringListConverter
import com.station.stationdownloader.data.datasource.local.room.dao.DownloadTaskDao
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.datasource.local.room.entities.DownloadTaskEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
@Database(
    entities = [DownloadTaskEntity::class, TorrentInfoEntity::class, TorrentFileInfoEntity::class],
    version = 1
)
@TypeConverters(
    StringListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadTaskDao(): DownloadTaskDao
    abstract fun torrentInfoDao():TorrentInfoDao
    abstract fun torrentFileInfoDao():TorrentFileInfoDao
}