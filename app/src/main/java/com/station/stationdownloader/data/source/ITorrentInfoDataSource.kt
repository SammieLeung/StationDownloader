package com.station.stationdownloader.data.source

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentInfoEntity

interface ITorrentInfoDataSource {
    suspend fun saveTorrentInfo(torrentInfo: XLTorrentInfoEntity):Long
    suspend fun saveTorrentFileInfo(torrentFileInfo: XLTorrentFileInfoEntity):Long
    suspend fun getTorrentId(hash:String):IResult<Long>
    suspend fun getTorrentFileInfo(torrentId:Long,realIndex:Int):IResult<XLTorrentFileInfoEntity>
}