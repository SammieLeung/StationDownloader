package com.station.stationdownloader.data.source.local.model

import androidx.work.Data
import androidx.work.workDataOf
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.DownloadWorker
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import java.io.Serializable

data class StationDownloadTask(
    val id: Long = 0L,
    val taskId: Long = 0L,
    val torrentId: Long = -1L,
    val url: String,
    val realUrl: String,
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
) : Serializable

fun StationDownloadTask.asXLDownloadTaskEntity(): XLDownloadTaskEntity {
    return XLDownloadTaskEntity(
        id = id,
        torrentId = torrentId,
        url = url,
        realUrl = realUrl,
        name = name,
        urlType = urlType,
        status = status,
        engine = engine,
        downloadPath = downloadPath,
        downloadSize = downloadSize,
        totalSize = totalSize,
        selectIndexes = selectIndexes,
        fileList = fileList,
        fileCount = fileCount,
        createTime = createTime
    )
}
