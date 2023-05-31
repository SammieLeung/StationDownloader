package com.station.stationdownloader.data.datasource

import com.xunlei.downloadlib.parameter.TorrentInfo

interface ITorrentInfoRepository {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfo):Long
}