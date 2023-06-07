package com.station.stationdownloader.data.datasource.model

import com.xunlei.downloadlib.parameter.XLTaskInfo


data class StationTaskInfo(
    val taskId: Long,
    val status: Int,
    val speed: Long,
    val downloadSize: Long,
    val totalSize: Long,
)

fun XLTaskInfo.asStationTaskInfo(): StationTaskInfo {
    return StationTaskInfo(
        taskId = this.mTaskId,
        status = this.mTaskStatus,
        speed = this.mDownloadSpeed,
        downloadSize = this.mDownloadSize,
        totalSize = this.mFileSize
    )
}