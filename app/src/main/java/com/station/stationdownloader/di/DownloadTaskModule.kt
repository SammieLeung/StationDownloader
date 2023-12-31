package com.station.stationdownloader.di

import android.content.Context
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.repository.DefaultDownloadTaskRepository
import com.station.stationdownloader.data.source.IDownloadTaskDataSource
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.aria2.Aria2Engine
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine
import com.station.stationdownloader.data.source.local.ConfigurationLocalDataSource
import com.station.stationdownloader.data.source.local.DownloadTaskLocalDataSource
import com.station.stationdownloader.data.source.local.TorrentInfoLocalDataSource
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfileManager
import com.station.stationdownloader.data.source.local.room.dao.XLDownloadTaskDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.source.remote.BtTrackerApiService
import com.station.stationdownloader.data.source.remote.FileSizeApiService
import com.station.stationdownloader.data.source.repository.DefaultConfigurationRepository
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.data.source.repository.DefaultTorrentInfoRepository
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class LocalConfigurationDataSource

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {
    @Provides
    @Singleton
    fun provideConfigurationRepo(
        @LocalConfigurationDataSource localDataSource: IConfigurationDataSource
    ): DefaultConfigurationRepository {
        return DefaultConfigurationRepository(localDataSource)
    }

    @Provides
    @Singleton
    @LocalConfigurationDataSource
    fun provideLocalConfigurationDataSource(
        defaultMMKV: MMKV,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): IConfigurationDataSource {
        return ConfigurationLocalDataSource(defaultMMKV, ioDispatcher)
    }
}

@InstallIn(SingletonComponent::class)
@Module
object DownloadTaskModule {
    @Singleton
    @Provides
    fun provideDownloadTaskRepository(
        localDataSource: IDownloadTaskDataSource,
        torrentDataSource: ITorrentInfoDataSource,
        @DefaultDispatcher
        defaultDispatcher: CoroutineDispatcher,
    ): IDownloadTaskRepository {
        return DefaultDownloadTaskRepository(localDataSource, torrentDataSource, defaultDispatcher)
    }

    @Provides
    fun provideDownloadTaskLocalDataSource(
        dao: XLDownloadTaskDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): IDownloadTaskDataSource {
        return DownloadTaskLocalDataSource(dao, ioDispatcher)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideEngineRepo(
        xlEngine: XLEngine,
        aria2Engine: Aria2Engine,
        downloadTaskRepo: IDownloadTaskRepository,
        configRepo: DefaultConfigurationRepository,
        torrentInfoRepo: ITorrentInfoRepository,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    ): DefaultEngineRepository {
        return DefaultEngineRepository(
            xlEngine = xlEngine,
            aria2Engine = aria2Engine,
            taskRepo = downloadTaskRepo,
            configRepo = configRepo,
            torrentInfoRepo = torrentInfoRepo,
            defaultDispatcher = defaultDispatcher,
        )
    }

    @Provides
    @Singleton
    fun provideXLEngine(
        @ApplicationContext context: Context,
        configRepo: DefaultConfigurationRepository,
        torrentInfoRepo: ITorrentInfoRepository,
        fileSizeApiService: FileSizeApiService,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): XLEngine {
        return XLEngine(
            context = context,
            configRepo = configRepo,
            torrentInfoRepo = torrentInfoRepo,
            fileSizeApiService = fileSizeApiService,
            defaultDispatcher = defaultDispatcher
        )
    }

    @Provides
    @Singleton
    fun provideAria2Engine(
        @ApplicationContext context: Context,
        profileManager: UserProfileManager,
        configRepo: DefaultConfigurationRepository,
        taskRepo: IDownloadTaskRepository,
        btTrackerApiService: BtTrackerApiService,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): Aria2Engine {
        return Aria2Engine(context, profileManager, configRepo, taskRepo, btTrackerApiService,defaultDispatcher)
    }

}

@Module
@InstallIn(SingletonComponent::class)
object TorrentInfoModule {

    @Provides
    fun provideTorrentInfoRepo(
        localDataSource: ITorrentInfoDataSource,
        @AppCoroutineScope externalScope: CoroutineScope
    ): ITorrentInfoRepository {
        return DefaultTorrentInfoRepository(localDataSource, externalScope)
    }

    @Provides
    fun provideTorrentInfoLocalDataSource(
        torrentInfoDao: TorrentInfoDao,
        torrentFileInfoDao: TorrentFileInfoDao,
        @IoDispatcher
        ioDispatcher: CoroutineDispatcher
    ): ITorrentInfoDataSource {
        return TorrentInfoLocalDataSource(torrentInfoDao, torrentFileInfoDao, ioDispatcher)
    }


}



