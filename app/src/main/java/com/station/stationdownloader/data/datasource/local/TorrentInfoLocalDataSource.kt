package com.station.stationdownloader.data.datasource.local

import com.station.stationdownloader.data.datasource.ITorrentInfoDataSource
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentInfoLocalDataSource(
    private val torrentInfoDao: TorrentInfoDao,
    private val torrentFileInfoDao: TorrentFileInfoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ITorrentInfoDataSource {
    override suspend fun saveTorrentInfo(torrentInfo: TorrentInfoEntity) =
        withContext(ioDispatcher) {
            torrentInfoDao.insertTorrentInfo(torrentInfo)
        }

    override suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity) =
        withContext(ioDispatcher) {
            torrentFileInfoDao.insertTorrentFileInfo(torrentFileInfo)
        }
}