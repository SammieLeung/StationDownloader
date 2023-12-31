package com.station.stationdownloader.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.station.stationdownloader.data.source.local.room.converter.StringListConverter
import com.station.stationdownloader.data.source.local.room.dao.XLDownloadTaskDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
@Database(
    entities = [XLDownloadTaskEntity::class, TorrentInfoEntity::class, TorrentFileInfoEntity::class],
    version = 1
)
@TypeConverters(
    StringListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getXLDownloadTaskDao(): XLDownloadTaskDao
    abstract fun getTorrentInfoDao():TorrentInfoDao
    abstract fun getTorrentFileInfoDao():TorrentFileInfoDao
}