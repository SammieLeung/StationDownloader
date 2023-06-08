package com.station.stationdownloader.di

import android.app.Application
import androidx.room.Room
import com.station.stationdownloader.data.datasource.local.room.AppDatabase
import com.station.stationdownloader.data.datasource.local.room.dao.XLDownloadTaskDao
import com.station.stationdownloader.data.datasource.local.room.dao.XLTorrentFileInfoDao
import com.station.stationdownloader.data.datasource.local.room.dao.XLTorrentInfoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * author: Sam Leung
 * date:  2023/5/15
 */

@InstallIn(SingletonComponent::class)
@Module
object DataBaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        application: Application
    ): AppDatabase {
        return Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "downloader.db"
        ).build()
    }

    @Provides
    fun provideDownloadTaskDao(
        database: AppDatabase
    ): XLDownloadTaskDao {
        return database.downloadTaskDao()
    }

    @Provides
    fun provideTorrentInfoDao(
        database: AppDatabase
    ): XLTorrentInfoDao {
        return database.torrentInfoDao()
    }

    @Provides
    fun provideTorrentFileInfoDao(
        database: AppDatabase
    ): XLTorrentFileInfoDao {
        return database.torrentFileInfoDao()
    }
}

