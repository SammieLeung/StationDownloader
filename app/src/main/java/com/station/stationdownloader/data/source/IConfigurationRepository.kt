package com.station.stationdownloader.data.source

import com.station.stationdownloader.DownloadEngine

interface IConfigurationRepository {
    suspend fun getSpeedLimit(): Long

    //下载路径
    suspend fun getDownloadPath(): String

    //多任务数量
    suspend fun getMaxThread(): Int

    suspend fun getDefaultEngine(): DownloadEngine
}