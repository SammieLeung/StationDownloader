package com.station.stationdownloader.data.source.local.engine

import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.TaskId
import com.station.stationdownloader.data.IResult

interface IEngine {
    suspend fun init(): IResult<String>
    suspend fun unInit()
    suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<String>
    suspend fun stopTask(taskId: String)
    suspend fun configure(key: String, values: String): IResult<String>
}