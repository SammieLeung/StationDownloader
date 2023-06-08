package com.station.stationdownloader.data.datasource.engine

import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.data.datasource.model.StationTaskInfo

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
interface IEngine {
    suspend fun init()
    suspend fun unInit()
    suspend fun initTask(url: String): IResult<StationDownloadTask>

    /**
     * 获取下载任务的大小
     */
    suspend fun getTaskSize(task: StationDownloadTask, timeOut: Long): IResult<StationDownloadTask>
     suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount:Int,
        selectIndexes:IntArray
    ):IResult<Long>
    suspend fun stopTask(task: StationDownloadTask)
    suspend fun getTaskInfo(taskId: Long): StationTaskInfo
    suspend fun configure(key: String, values: Array<String>): IResult<Unit>
}

