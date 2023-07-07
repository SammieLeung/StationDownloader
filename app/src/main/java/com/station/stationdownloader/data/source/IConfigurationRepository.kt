package com.station.stationdownloader.data.source

import com.station.stationdownloader.DownloadEngine

interface IConfigurationRepository {
    suspend fun getDownloadSpeedLimit(): Long
    suspend fun setDownloadSpeedLimit(downloadSpeedLimit: Long)
    suspend fun getUploadSpeedLimit(): Long
    suspend fun setUploadSpeedLimit(uploadSpeedLimit: Long)
    suspend fun getSpeedLimit(): Long
    suspend fun setSpeedLimit(speedLimit: Long)

    //下载路径
    suspend fun getDownloadPath(): String
    suspend fun setDownloadPath(path: String)

    //多任务数量
    suspend fun getMaxThread(): Int
    suspend fun setMaxThread(count: Int)

    suspend fun setDefaultEngine(engine: DownloadEngine)
    suspend fun getDefaultEngine(): DownloadEngine
}