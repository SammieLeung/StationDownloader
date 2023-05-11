package com.station.stationdownloader.data.datasource.model


data class StationTorrentFileInfo(
    var fileIndex: Int = 0,
    var fileName: String = "",
    var fileSize: Long = 0,
    var realIndex: Int = 0,
    var subPath: String = "",
)
