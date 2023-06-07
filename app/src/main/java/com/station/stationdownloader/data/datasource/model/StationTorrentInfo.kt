package com.station.stationdownloader.data.datasource.model

import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentInfo
import java.io.Serializable

data class StationTorrentInfo(
    var fileCount: Int = 0,
    var hash: String,
    var isMultiFiles: Boolean = false,
    var multiFileBaseFolder: String = "",
    var subFileInfo: List<StationTorrentFileInfo> = emptyList()
): Serializable

fun StationTorrentInfo.asTorrentInfoEntity(): TorrentInfoEntity {
    return TorrentInfoEntity(
        id = 0,
        fileCount = this.fileCount,
        hash = this.hash,
        isMultiFiles = this.isMultiFiles,
        multiFileBaseFolder = this.multiFileBaseFolder
    )
}

