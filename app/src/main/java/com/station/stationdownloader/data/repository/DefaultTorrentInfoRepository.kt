package com.station.stationdownloader.data.repository

import com.station.stationdownloader.data.datasource.ITorrentInfoDataSource
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.station.stationdownloader.data.datasource.local.TorrentInfoLocalDataSource
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultTorrentInfoRepository(
    private val localDataSource: ITorrentInfoDataSource,
) : ITorrentInfoRepository {
    override suspend fun saveTorrentInfo(torrentInfo: TorrentInfoEntity) {
        localDataSource.saveTorrentInfo(torrentInfo)
    }

    override suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity) {
        localDataSource.saveTorrentFileInfo(torrentFileInfo)
    }

}