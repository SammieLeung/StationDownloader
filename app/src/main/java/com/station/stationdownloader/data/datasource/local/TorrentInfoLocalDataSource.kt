package com.station.stationdownloader.data.datasource.local

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.ITorrentInfoDataSource
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.datasource.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentFileInfo
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

    override suspend fun getTorrentId(hash: String): IResult<Long> = withContext(ioDispatcher) {
        val tId = torrentInfoDao.getTorrentId(hash)
        if (tId > 0L) {
            return@withContext IResult.Success(tId)
        }
        return@withContext IResult.Error(Exception("torrentId Error"))
    }

    override suspend fun getTorrentFileInfo(
        torrentId: Long,
        realIndex: Int
    ): IResult<TorrentFileInfoEntity> =
        withContext(ioDispatcher) {
            val fileInfo = torrentFileInfoDao.getTorrentFileInfo(torrentId, realIndex)
            if (fileInfo != null) {
                IResult.Success(fileInfo)
            } else {
                IResult.Error(Exception("TorrentFileInfo not found!"))
            }
        }
}