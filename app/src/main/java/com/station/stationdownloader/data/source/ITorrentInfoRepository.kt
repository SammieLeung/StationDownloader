package com.station.stationdownloader.data.source

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentInfo

interface ITorrentInfoRepository {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfo):Long
    suspend fun getTorrentByHash(hash: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>
}