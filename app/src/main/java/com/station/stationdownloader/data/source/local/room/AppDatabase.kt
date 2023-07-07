package com.station.stationdownloader.data.source.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.station.stationdownloader.data.source.local.room.converter.StringListConverter
import com.station.stationdownloader.data.source.local.room.dao.XLDownloadTaskDao
import com.station.stationdownloader.data.source.local.room.dao.XLTorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.XLTorrentInfoDao
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentInfoEntity

/**
 * author: Sam Leung
 * date:  2023/5/15
 */
@Database(
    entities = [XLDownloadTaskEntity::class, XLTorrentInfoEntity::class, XLTorrentFileInfoEntity::class],
    version = 1
)
@TypeConverters(
    StringListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getXLDownloadTaskDao(): XLDownloadTaskDao
    abstract fun getXLTorrentInfoDao():XLTorrentInfoDao
    abstract fun getXLTorrentFileInfoDao():XLTorrentFileInfoDao
}