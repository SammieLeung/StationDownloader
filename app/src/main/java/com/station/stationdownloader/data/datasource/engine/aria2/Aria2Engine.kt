package com.station.stationdownloader.data.datasource.engine.aria2

import com.station.stationdownloader.data.datasource.engine.ExecuteResult
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.model.StationDownloadTask

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class Aria2Engine(): IEngine {
    override fun init() {
        TODO("Not yet implemented")
    }

    override fun unInit() {
        TODO("Not yet implemented")
    }

    override fun initTask(url: String): ExecuteResult<StationDownloadTask> {
        TODO("Not yet implemented")
    }

    override fun startTask(task: StationDownloadTask) {
        TODO("Not yet implemented")
    }

    override fun stopTask(task: StationDownloadTask) {
        TODO("Not yet implemented")
    }

    override fun configure(key: String, values: Array<String>): ExecuteResult<Nothing> {
        TODO("Not yet implemented")
    }
}