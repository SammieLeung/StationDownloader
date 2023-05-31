package com.station.stationdownloader.data.datasource.model

import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.xunlei.downloadlib.parameter.TorrentFileInfo


data class StationTorrentFileInfo(
    var fileIndex: Int = 0,
    var fileName: String = "",
    var fileSize: Long = 0,
    var realIndex: Int = 0,
    var subPath: String = "",
)

fun StationTorrentFileInfo.asTorrentFileInfoEntity(torrentId: Long): TorrentFileInfoEntity {
    return TorrentFileInfoEntity(
        id = 0,
        torrentId = torrentId,
        fileIndex = this.fileIndex,
        fileName = this.fileName,
        fileSize = this.fileSize,
        realIndex = this.realIndex,
        subPath = this.subPath
    )
}

