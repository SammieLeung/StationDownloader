package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.model.StationDownloadTask

interface IEngineRepository {
    suspend fun init(): IResult<Unit>

    suspend fun unInit(): IResult<Unit>
    suspend fun initTask(url: String): IResult<StationDownloadTask>

    fun configure(key: String, values: Array<String>): IResult<Unit>

}

