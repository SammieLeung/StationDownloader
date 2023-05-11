package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.DownloadEngine

interface IConfigurationDataSource {
    fun getDownloadSpeedLimit():Long
    fun setDownloadSpeedLimit(downloadSpeedLimit:Long)
    fun getUploadSpeedLimit():Long
    fun setUploadSpeedLimit(uploadSpeedLimit:Long)
    fun getSpeedLimit():Long
    fun setSpeedLimit(speedLimit:Long)
    fun getDownloadPath():String
    fun setDownloadPath(path:String)
    fun getMaxThread():Int
    fun setMaxThread(count:Int)
    fun setDefaultEngine(engine: DownloadEngine)
    fun getDefaultEngine(): DownloadEngine
}
