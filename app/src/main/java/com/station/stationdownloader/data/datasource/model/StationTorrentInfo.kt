package com.station.stationdownloader.data.datasource.model

import com.station.stationdownloader.data.datasource.local.room.entities.XLTorrentInfoEntity
import java.io.Serializable

data class StationTorrentInfo(
    var fileCount: Int = 0,
    var hash: String,
    var isMultiFiles: Boolean = false,
    var multiFileBaseFolder: String = "",
    var subFileInfo: List<StationTorrentFileInfo> = emptyList()
): Serializable

fun StationTorrentInfo.asTorrentInfoEntity(): XLTorrentInfoEntity {
    return XLTorrentInfoEntity(
        id = 0,
        fileCount = this.fileCount,
        hash = this.hash,
        isMultiFiles = this.isMultiFiles,
        multiFileBaseFolder = this.multiFileBaseFolder
    )
}

