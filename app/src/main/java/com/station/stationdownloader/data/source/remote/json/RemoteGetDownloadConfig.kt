package com.station.stationdownloader.data.source.remote.json

data class RemoteGetDownloadConfig(
    val speed_limit:Long,
    val max_download_thread:Int,
    val download_path:String,
)
