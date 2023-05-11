package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity

interface ITorrentInfoRepository {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfoEntity)
    suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity)
}