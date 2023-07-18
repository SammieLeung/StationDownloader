package com.station.stationdownloader.di

import android.content.Context
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.repository.DefaultDownloadTaskRepository
import com.station.stationdownloader.data.source.IDownloadTaskDataSource
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.aria2.Aria2Engine
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine
import com.station.stationdownloader.data.source.local.ConfigurationLocalDataSource
import com.station.stationdownloader.data.source.local.DownloadTaskLocalDataSource
import com.station.stationdownloader.data.source.local.TorrentInfoLocalDataSource
import com.station.stationdownloader.data.source.local.room.dao.XLDownloadTaskDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentInfoDao
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


@Qualifier
annotation class XLEngineAnnotation

@Qualifier
annotation class Aria2EngineAnnotation


@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {
    @Provides
    @Singleton
    fun provideConfigurationRepo(
        @XLEngineAnnotation xlEngine: IEngine,
        @Aria2EngineAnnotation aria2Engine: IEngine,
        @LocalConfigurationDataSource localDataSource: IConfigurationDataSource
    ): IConfigurationRepository {
        return DefaultConfigurationRepository(xlEngine, aria2Engine, localDataSource)
    }

    @Provides
    @Singleton
    @LocalConfigurationDataSource
    fun provideLocalConfigurationDataSource(
        defaultMMKV: MMKV,
    ): IConfigurationDataSource {
        return ConfigurationLocalDataSource(defaultMMKV)
    }
}

@InstallIn(SingletonComponent::class)
@Module
object DownloadTaskModule {
    @Singleton
    @Provides
    fun provideDownloadTaskRepository(
        localDataSource: IDownloadTaskDataSource,
    ): IDownloadTaskRepository {
        return DefaultDownloadTaskRepository(localDataSource)
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
        @XLEngineAnnotation xlEngine: IEngine,
        @Aria2EngineAnnotation aria2Engine: IEngine,
        downloadTaskRepo: IDownloadTaskRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): IEngineRepository {
        return DefaultEngineRepository(
            xlEngine = xlEngine,
            aria2Engine = aria2Engine,
            downloadTaskRepo = downloadTaskRepo,
            ioDispatcher = ioDispatcher
        )
    }

    @Provides
    @Singleton
    @XLEngineAnnotation
    fun provideXLEngine(
        @ApplicationContext context: Context,
        @LocalConfigurationDataSource configurationDataSource: IConfigurationDataSource,
        torrentInfoRepo: ITorrentInfoRepository,
        fileSizeApiService: FileSizeApiService,
        @AppCoroutineScope externalScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): IEngine {
        return XLEngine(
            context = context,
            configurationDataSource = configurationDataSource,
            torrentInfoRepo = torrentInfoRepo,
            fileSizeApiService = fileSizeApiService,
            externalScope = externalScope,
            ioDispatcher = ioDispatcher,
            defaultDispatcher = defaultDispatcher
        )
    }

    @Provides
    @Singleton
    @Aria2EngineAnnotation
    fun provideAria2Engine(
        @ApplicationContext context: Context,
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): IEngine {
        return Aria2Engine(context, defaultDispatcher)
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



