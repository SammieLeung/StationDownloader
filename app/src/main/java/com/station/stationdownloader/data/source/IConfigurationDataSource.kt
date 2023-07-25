package com.station.stationdownloader.data.source

import com.station.stationdownloader.DownloadEngine

interface IConfigurationDataSource {
    suspend fun getSpeedLimit(): Long
    suspend fun setSpeedLimit(speedLimit: Long): Boolean
    suspend fun getDownloadPath(): String
    suspend fun setDownloadPath(path: String): Boolean
    suspend fun getMaxThread(): Int
    suspend fun setMaxThread(count: Int): Boolean
    suspend fun setDefaultEngine(engine: DownloadEngine): Boolean
    suspend fun getDefaultEngine(): DownloadEngine
}
