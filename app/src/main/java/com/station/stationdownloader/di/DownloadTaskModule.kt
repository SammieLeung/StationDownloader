package com.station.stationdownloader.di

import android.content.Context
import com.station.stationdownloader.data.datasource.IConfigurationDataSource
import com.station.stationdownloader.data.datasource.IConfigurationRepository
import com.station.stationdownloader.data.datasource.IDownloadTaskRepository
import com.station.stationdownloader.data.repository.DefaultDownloadTaskRepository
import com.station.stationdownloader.data.datasource.IDownloadTaskDataSource
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.ITorrentInfoDataSource
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.engine.aria2.Aria2Engine
import com.station.stationdownloader.data.datasource.engine.xl.XLEngine
import com.station.stationdownloader.data.datasource.local.ConfigurationLocalDataSource
import com.station.stationdownloader.data.datasource.local.DownloadTaskLocalDataSource
import com.station.stationdownloader.data.datasource.local.TorrentInfoLocalDataSource
import com.station.stationdownloader.data.datasource.local.room.dao.DownloadTaskDao
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.repository.DefaultConfigurationRepository
import com.station.stationdownloader.data.repository.DefaultEngineRepository
import com.station.stationdownloader.data.repository.DefaultTorrentInfoRepository
import com.tencent.mmkv.MMKV
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class LocalConfigurationDataSource

@Qualifier
annotation class ConfigurationRepo

@Qualifier
annotation class XLEngineAnnotation

@Qualifier
annotation class Aria2EngineAnnotation


@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {
    @Provides
    @Singleton
    @ConfigurationRepo
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
        dao: DownloadTaskDao,
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
        @ConfigurationRepo configurationRepo: IConfigurationRepository,
        downloadTaskRepo: IDownloadTaskRepository,
        torrentInfoRepo: ITorrentInfoRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): IEngineRepository {
        return DefaultEngineRepository(
            xlEngine = xlEngine,
            aria2Engine = aria2Engine,
            configurationRepo = configurationRepo,
            downloadTaskRepo = downloadTaskRepo,
            torrentInfoRepo = torrentInfoRepo,
            ioDispatcher = ioDispatcher
        )
    }

    @Provides
    @Singleton
    @XLEngineAnnotation
    fun provideXLEngine(
        @ApplicationContext context: Context,
    ): IEngine {
        return XLEngine(
            context = context
        )
    }

    @Provides
    @Singleton
    @Aria2EngineAnnotation
    fun provideAria2Engine(
    ): IEngine {
        return Aria2Engine()
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



