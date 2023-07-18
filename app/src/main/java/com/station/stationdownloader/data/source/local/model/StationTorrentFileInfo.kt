package com.station.stationdownloader.data.source.local.model

import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import java.io.Serializable


data class StationTorrentFileInfo(
    var fileIndex: Int = 0,
    var fileName: String = "",
    var fileSize: Long = 0,
    var realIndex: Int = 0,
    var subPath: String = "",
    var isChecked:Boolean=false
): Serializable

fun StationTorrentFileInfo.asXLTorrentFileInfoEntity(torrentId: Long): TorrentFileInfoEntity {
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

