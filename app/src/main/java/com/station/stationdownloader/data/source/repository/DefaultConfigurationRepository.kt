package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.station.stationdownloader.data.source.IConfigurationRepository

class DefaultConfigurationRepository(
    private val localDataSource: IConfigurationDataSource
) : IConfigurationRepository {
    override suspend fun getSpeedLimit(): Long {
        return localDataSource.getSpeedLimit()
    }


    override suspend fun getDownloadPath(): String {
        return localDataSource.getDownloadPath()
    }

    override suspend fun getMaxThread(): Int {
        return localDataSource.getMaxThread()
    }

    override suspend fun getDefaultEngine(): DownloadEngine {
        return localDataSource.getDefaultEngine()
    }
}

