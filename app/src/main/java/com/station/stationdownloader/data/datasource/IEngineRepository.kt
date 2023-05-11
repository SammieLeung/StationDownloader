package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.data.datasource.engine.ExecuteResult
import com.station.stationdownloader.data.datasource.model.StationDownloadTask

interface IEngineRepository {
    fun init():ExecuteResult<Nothing>

    fun unInit():ExecuteResult<Nothing>
    fun initTask(url:String): ExecuteResult<StationDownloadTask>

    fun configure(key:String,values:Array<String>):ExecuteResult<Nothing>

}

