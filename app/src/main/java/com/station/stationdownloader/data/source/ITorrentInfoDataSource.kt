package com.station.stationdownloader.data.source

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentFileInfo

interface ITorrentInfoDataSource {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfoEntity):Long
    suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity):Long
    suspend fun getTorrentId(hash:String):IResult<Long>
    suspend fun getTorrentFileInfo(torrentId:Long,realIndex:Int):IResult<TorrentFileInfoEntity>
    suspend fun getTorrentByHash(hash: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>
}