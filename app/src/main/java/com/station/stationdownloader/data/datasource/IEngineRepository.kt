package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.data.datasource.model.StationTaskInfo

interface IEngineRepository {
    suspend fun init(): IResult<Unit>

    suspend fun unInit(): IResult<Unit>
    suspend fun initTask(url: String): IResult<StationDownloadTask>
    suspend fun startTask(
        url: String,
        engine: DownloadEngine = DownloadEngine.XL,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount:Int,
        selectIndexes: IntArray
    ): IResult<Long>

    suspend fun getTaskSize(
        startDownloadTask: StationDownloadTask,
        timeOut: Long
    ): IResult<StationDownloadTask>

    suspend fun getTaskInfo(taskId: Long): StationTaskInfo
    suspend fun configure(key: String, values: Array<String>): IResult<Unit>

}

