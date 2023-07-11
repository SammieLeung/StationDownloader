package com.station.stationdownloader.data.source

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.StationTaskInfo

interface IEngineRepository {
    suspend fun init(): IResult<Unit>

    suspend fun unInit(): IResult<Unit>
    suspend fun initUrl(url: String): IResult<NewTaskConfigModel>
    suspend fun startTask(
        url: String,
        engine: DownloadEngine = DownloadEngine.XL,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount:Int,
        selectIndexes: IntArray
    ): IResult<Long>

    suspend fun configure(key: String, values: Array<String>): IResult<Unit>

}

