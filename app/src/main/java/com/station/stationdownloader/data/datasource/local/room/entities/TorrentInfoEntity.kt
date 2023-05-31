package com.station.stationdownloader.data.datasource.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.datasource.model.StationTorrentFileInfo
import com.station.stationdownloader.data.datasource.model.StationTorrentInfo
import com.xunlei.downloadlib.parameter.TorrentInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Entity(
    tableName = "torrent_info",
    indices = [Index(value = ["hash"], unique = true)]
)
data class TorrentInfoEntity @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    @ColumnInfo(name = "file_count", defaultValue = "0")
    var fileCount: Int = 0,
    var hash: String,
    @ColumnInfo(name = "is_multi_files")
    var isMultiFiles: Boolean = false,
    @ColumnInfo(name = "multi_file_base_folder")
    var multiFileBaseFolder: String = "",
)

fun TorrentInfo.asTorrentInfoEntity(): TorrentInfoEntity {
    return TorrentInfoEntity(
        id = 0,
        fileCount = this.mFileCount,
        hash = this.mInfoHash,
        isMultiFiles = this.mIsMultiFiles,
        multiFileBaseFolder = this.mMultiFileBaseFolder
    )
}

 fun TorrentInfo.asStationTorrentInfo(): StationTorrentInfo {
    return StationTorrentInfo(
        fileCount = this.mFileCount,
        hash = this.mInfoHash,
        isMultiFiles = this.mIsMultiFiles,
        multiFileBaseFolder = this.mMultiFileBaseFolder,
        subFileInfo = this.mSubFileInfo.map { it.asStationTorrentFileInfo() }
    )
}