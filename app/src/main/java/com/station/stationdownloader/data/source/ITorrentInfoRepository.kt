package com.station.stationdownloader.data.source

import com.xunlei.downloadlib.parameter.TorrentInfo

interface ITorrentInfoRepository {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfo):Long
}