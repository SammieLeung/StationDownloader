package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.DownloadEngine

interface IConfigurationRepository {
    fun getDownloadSpeedLimit():Long
    fun setDownloadSpeedLimit(downloadSpeedLimit:Long)
    fun getUploadSpeedLimit():Long
    fun setUploadSpeedLimit(uploadSpeedLimit:Long)
    fun getSpeedLimit():Long
    fun setSpeedLimit(speedLimit:Long)
    //下载路径
    fun getDownloadPath():String
    fun setDownloadPath(path:String)
    //多任务数量
    fun getMaxThread():Int
    fun setMaxThread(count:Int)

    fun setDefaultEngine(engine:DownloadEngine)
    fun getDefaultEngine():DownloadEngine
}