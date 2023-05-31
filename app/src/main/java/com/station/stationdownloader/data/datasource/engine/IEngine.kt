package com.station.stationdownloader.data.datasource.engine

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.model.StationDownloadTask

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
interface IEngine {
        fun init()
        fun unInit()
        fun initTask(url:String): IResult<StationDownloadTask>
        fun startTask(task: StationDownloadTask)
        fun stopTask(task: StationDownloadTask)
        fun configure(key:String,values:Array<String>):IResult<Unit>
}

