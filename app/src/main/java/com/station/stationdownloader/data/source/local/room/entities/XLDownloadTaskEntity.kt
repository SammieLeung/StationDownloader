package com.station.stationdownloader.data.source.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import org.jetbrains.annotations.NotNull

/**
 * author: Sam Leung
 * date:  2023/5/10
 */
@Entity(
    tableName = "xl_download_task",
    indices = [Index(value = ["url", "engine", "download_path"], unique = true)]
)
data class XLDownloadTaskEntity @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    @ColumnInfo(defaultValue = "-1", name = "torrent_id")
    val torrentId: Long = -1,
    @NotNull
    val url: String,
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

