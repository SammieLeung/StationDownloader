package com.station.stationdownloader.data.datasource

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentFileInfo
import com.xunlei.downloadlib.parameter.TorrentInfo

interface ITorrentInfoDataSource {
    suspend fun saveTorrentInfo(torrentInfo: TorrentInfoEntity):Long
    suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity):Long
    suspend fun getTorrentId(hash:String):IResult<Long>
    suspend fun getTorrentFileInfo(torrentId:Long,realIndex:Int):IResult<TorrentFileInfoEntity>
}