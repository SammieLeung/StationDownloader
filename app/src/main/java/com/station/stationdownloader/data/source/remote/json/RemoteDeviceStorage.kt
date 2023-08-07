package com.station.stationdownloader.data.source.remote.json

data class RemoteDeviceStorage(
    val path: String,
    val avaiable_size: Long,
    val total_size: Long
)