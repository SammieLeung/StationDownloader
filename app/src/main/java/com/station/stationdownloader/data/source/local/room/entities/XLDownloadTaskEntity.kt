package com.station.stationdownloader.data.source.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.work.Data
import androidx.work.workDataOf
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.DownloadWorker
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import org.jetbrains.annotations.NotNull

/**
 * author: Sam Leung
 * date:  2023/5/10
 */
@Entity(
    tableName = "xl_download_task",
    indices = [Index(value = ["url"], unique = true)]
)
data class XLDownloadTaskEntity @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(defaultValue = "-1", name = "torrent_id")
    val torrentId: Long = -1,
    @NotNull
    val url: String,
    @ColumnInfo(name="real_url")
    val realUrl:String,
    val name: String = "",
    @ColumnInfo(defaultValue = "PENDING")
    val status: DownloadTaskStatus = DownloadTaskStatus.PENDING,
    @ColumnInfo(name = "url_type", defaultValue = "UNKNOWN")
    val urlType: DownloadUrlType = DownloadUrlType.UNKNOWN,
    val engine: DownloadEngine = DownloadEngine.XL,
    @ColumnInfo(name = "download_size")
    val downloadSize: Long = -1,
    @ColumnInfo(name = "total_size")
    val totalSize: Long = -1,
    @ColumnInfo(name = "download_path")
    val downloadPath: String = "",
    //TODO @Deprecate
    @ColumnInfo(name = "select_indexes")
    val selectIndexes: List<Int> = emptyList(),
    //TODO @Deprecate
    @ColumnInfo(name = "file_list")
    val fileList: List<String> = emptyList(),
    //TODO @Deprecate
    @ColumnInfo(name = "file_count")
    val fileCount: Int = 0,
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
)

fun XLDownloadTaskEntity.buildWorkInputData(): Data {
    return workDataOf(
        DownloadWorker.IN_URL to url,
        DownloadWorker.IN_ENGINE to engine.name,
        DownloadWorker.IN_DOWNLOAD_PATH to downloadPath
    )
}

fun XLDownloadTaskEntity.asStationDownloadTask(): StationDownloadTask {
    return StationDownloadTask(
        id=id,
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
