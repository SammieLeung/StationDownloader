package com.station.stationdownloader.data.source

import com.station.stationdownloader.contants.Options

interface IConfigurationDataSource {
    suspend fun getValue(key: Options): String
    suspend fun setValue(key: Options, value: String): Boolean
}
