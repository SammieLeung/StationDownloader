package com.station.stationdownloader.data.source.local

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.local.room.dao.TorrentFileInfoDao
import com.station.stationdownloader.data.source.local.room.dao.TorrentInfoDao
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentInfoLocalDataSource internal constructor(
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


    override suspend fun getTorrentId(hash: String,torrentPath:String): IResult<Long> = withContext(ioDispatcher) {
        val tId = torrentInfoDao.getTorrentId(hash,torrentPath)
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

    override suspend fun getTorrentByHash(hash: String,torrentPath:String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
        withContext(ioDispatcher) {
            val result = torrentInfoDao.getTorrentByHash(hash,torrentPath)
            IResult.Success(result)
        }

    override suspend fun getTorrentById(torrentId: Long): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
        withContext(ioDispatcher) {
            val result = torrentInfoDao.getTorrentById(torrentId)
            if (result.isNotEmpty()) {
                return@withContext IResult.Success(result)
            } else {
                return@withContext IResult.Error(Exception("TorrentInfo not found!"))
            }
        }

}