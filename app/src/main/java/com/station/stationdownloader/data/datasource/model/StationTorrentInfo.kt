package com.station.stationdownloader.data.datasource.model

data class StationTorrentInfo(
    var fileCount: Int = 0,
    var hash: String,
    var isMultiFiles: Boolean = false,
    var multiFileBaseFolder: String = "",
    var subFileInfo: List<StationTorrentFileInfo> = emptyList()
)

