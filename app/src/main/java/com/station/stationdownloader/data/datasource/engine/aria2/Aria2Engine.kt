package com.station.stationdownloader.data.datasource.engine.aria2

import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.data.datasource.model.StationTaskInfo

/**
 * author: Sam Leung
 * date:  2023/5/9
 */
class Aria2Engine internal constructor(): IEngine {
    override suspend fun init() {
        TODO("Not yet implemented")
    }

    override suspend fun unInit() {
        TODO("Not yet implemented")
    }

    override suspend fun initTask(url: String): IResult<StationDownloadTask> {
        TODO("Not yet implemented")
    }

    override suspend fun getTaskSize(
        task: StationDownloadTask,
        timeOut: Long
    ): IResult<StationDownloadTask> {
        TODO("Not yet implemented")
    }

    override suspend fun startTask(
        url: String,
        downloadPath: String,
        name: String,
        urlType: DownloadUrlType,
        fileCount: Int,
        selectIndexes: IntArray
    ): IResult<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun stopTask(task: StationDownloadTask) {
        TODO("Not yet implemented")
    }

    override suspend fun getTaskInfo(taskId: Long): StationTaskInfo {
        TODO("Not yet implemented")
    }

    override suspend fun configure(key: String, values: Array<String>): IResult<Unit> {
        TODO("Not yet implemented")
    }


}