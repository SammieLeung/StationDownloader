package com.station.stationdownloader.data.source.local

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.local.room.dao.XLTorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.XLTorrentInfoDao
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.XLTorrentInfoEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentInfoLocalDataSource internal constructor(
    private val torrentInfoDao: XLTorrentInfoDao,
    private val torrentFileInfoDao: XLTorrentFileInfoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ITorrentInfoDataSource {
    override suspend fun saveTorrentInfo(torrentInfo: XLTorrentInfoEntity) =
        withContext(ioDispatcher) {
            torrentInfoDao.insertTorrentInfo(torrentInfo)
        }

    override suspend fun saveTorrentFileInfo(torrentFileInfo: XLTorrentFileInfoEntity) =
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
    ): IResult<XLTorrentFileInfoEntity> =
        withContext(ioDispatcher) {
            val fileInfo = torrentFileInfoDao.getTorrentFileInfo(torrentId, realIndex)
            if (fileInfo != null) {
                IResult.Success(fileInfo)
            } else {
                IResult.Error(Exception("TorrentFileInfo not found!"))
            }
        }
}