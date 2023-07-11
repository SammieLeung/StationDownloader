package com.station.stationdownloader.data.source.local.engine

import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.StationTaskInfo
import com.station.stationdownloader.utils.TaskTools

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
interface IEngine {
    suspend fun init()
    suspend fun unInit()
    suspend fun initUrl(url: String): IResult<NewTaskConfigModel>

    /**
     * 获取下载任务的大小
     */
     suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount:Int,
        selectIndexes:IntArray
    ):IResult<Long>
    suspend fun stopTask(task: StationDownloadTask)
    suspend fun configure(key: String, values: Array<String>): IResult<Unit>


}

