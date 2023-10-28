package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.contants.Options
import com.station.stationdownloader.data.source.IConfigurationDataSource

class DefaultConfigurationRepository(
    private val localDataSource: IConfigurationDataSource
) {

    suspend fun getValue(key: Options): String {
        return localDataSource.getValue(key)
    }

    suspend fun setValue(key: Options, value: String): Boolean {
        return localDataSource.setValue(key, value)
    }
}

