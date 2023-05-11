package com.station.stationdownloader.data.datasource.model

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType

data class StationDownloadTask(
    val taskId: Long = 0L,
    val url: String,
    val name: String,
    val status: DownloadTaskStatus = DownloadTaskStatus.PENDING,
    val urlType: DownloadUrlType = DownloadUrlType.UNKNOWN,
    val engine: DownloadEngine = DownloadEngine.XL,
    val downloadSize: Long = -1L,
    val totalSize: Long = -1L,
    val downloadPath: String,
    val selectIndexes: List<Int> = emptyList(),
    val fileList: List<String> = emptyList(),
    val fileCount: Int = 0,
    val createTime: Long = System.currentTimeMillis()
)

