package com.station.stationdownloader.data.source

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentInfo

interface ITorrentInfoRepository {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfo, torrentPath: String):IResult<Long>
    suspend fun getTorrentInfoById(torrentId: Long): IResult<TorrentInfoEntity>
    suspend fun getTorrentByHash(hash: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>
    suspend fun getTorrentHash(torrentId: Long): IResult<String>
    suspend fun getTorrentById(torrentId:Long):IResult<Map<TorrentInfoEntity,List<TorrentFileInfoEntity>>>
    suspend fun getTorrentByPath(torrentPath:String):IResult<Map<TorrentInfoEntity,List<TorrentFileInfoEntity>>>
}