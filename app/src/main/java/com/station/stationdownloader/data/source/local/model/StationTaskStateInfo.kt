package com.station.stationdownloader.data.source.local.model

import com.xunlei.downloadlib.parameter.XLTaskInfo


data class StationTaskStateInfo(
    val taskId: Long,
    val status: Int,
    val speed: Long,
    val downloadSize: Long,
    val totalSize: Long,
)

fun XLTaskInfo.asStationTaskInfo(): StationTaskStateInfo {
    return StationTaskStateInfo(
        taskId = this.mTaskId,
        status = this.mTaskStatus,
        speed = this.mDownloadSpeed,
        downloadSize = this.mDownloadSize,
        totalSize = this.mFileSize
    )
}