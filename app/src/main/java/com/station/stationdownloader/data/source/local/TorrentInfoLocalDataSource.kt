package com.station.stationdownloader.data.source.local

import com.station.stationdownloader.contants.TaskExecuteError
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
            val torrentId = torrentInfoDao.insertTorrentInfo(torrentInfo)
            if (torrentId > 0) {
                return@withContext IResult.Success(torrentId)
            } else {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.INSERT_TORRENT_INFO_FAILED.name),
                    TaskExecuteError.INSERT_TORRENT_INFO_FAILED.ordinal
                )
            }
        }

    override suspend fun saveTorrentFileInfo(torrentFileInfo: TorrentFileInfoEntity) =
        withContext(ioDispatcher) {
            val torrentFileInfoId = torrentFileInfoDao.insertTorrentFileInfo(torrentFileInfo)
            if (torrentFileInfoId > 0) {
                return@withContext IResult.Success(torrentFileInfoId)
            } else {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.INSERT_TORRENT_FILE_INFO_FAILED.name),
                    TaskExecuteError.INSERT_TORRENT_FILE_INFO_FAILED.ordinal
                )
            }
        }


    override suspend fun getTorrentId(hash: String, torrentPath: String): IResult<Long> =
        withContext(ioDispatcher) {
            val tId = torrentInfoDao.getTorrentId(hash, torrentPath)
            if (tId > 0L) {
                return@withContext IResult.Success(tId)
            }
            return@withContext IResult.Error(
                Exception(TaskExecuteError.QUERY_TORRENT_ID_FAILED.name),
                TaskExecuteError.QUERY_TORRENT_ID_FAILED.ordinal
            )
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
                IResult.Error(
                    Exception(TaskExecuteError.QUERY_TORRENT_FILE_INFO_FAILED.name),
                    TaskExecuteError.QUERY_TORRENT_FILE_INFO_FAILED.ordinal
                )
            }
        }

    override suspend fun getTorrentByHash(
        hash: String,
        torrentPath: String
    ): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
        withContext(ioDispatcher) {
            val result = torrentInfoDao.getTorrentByHash(hash, torrentPath)
            IResult.Success(result)
        }

    override suspend fun getTorrentById(torrentId: Long): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
        withContext(ioDispatcher) {
            val result = torrentInfoDao.getTorrentById(torrentId)
            if (result.isNotEmpty()) {
                return@withContext IResult.Success(result)
            } else {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.QUERY_TORRENT_FAILED.name),
                    TaskExecuteError.QUERY_TORRENT_FAILED.ordinal
                )
            }
        }

    override suspend fun getTorrentByPath(torrentPath: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
        withContext(ioDispatcher) {
            val result = torrentInfoDao.getTorrentByPath(torrentPath)
            if (result.isNotEmpty()) {
                return@withContext IResult.Success(result)
            } else {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.QUERY_TORRENT_FAILED.name),
                    TaskExecuteError.QUERY_TORRENT_FAILED.ordinal
                )
            }
        }

}