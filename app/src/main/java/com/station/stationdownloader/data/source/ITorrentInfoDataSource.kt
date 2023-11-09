package com.station.stationdownloader.data.source

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentFileInfo

interface ITorrentInfoDataSource {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfoEntity): IResult<Long>
    suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity): IResult<Long>
    suspend fun updateTorrentInfo(torrentInfo:TorrentInfoEntity):IResult<Int>
    suspend fun getTorrentId(hash: String, torrentPath: String): IResult<Long>
    suspend fun getTorrentInfoByHash(hash: String): IResult<TorrentInfoEntity>
    suspend fun getTorrentInfoById(torrentId: Long): IResult<TorrentInfoEntity>

    suspend fun getTorrentFileInfo(torrentId: Long, realIndex: Int): IResult<TorrentFileInfoEntity>
    suspend fun getTorrentByHash(
        hash: String,
    ): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>

    suspend fun getTorrentById(torrentId: Long): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>
    suspend fun getTorrentByPath(torrentPath: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>
}