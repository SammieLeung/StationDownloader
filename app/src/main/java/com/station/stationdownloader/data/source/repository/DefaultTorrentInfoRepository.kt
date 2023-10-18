package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asTorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asTorrentInfoEntity
import com.station.stationdownloader.utils.DLogger
import com.xunlei.downloadlib.parameter.TorrentInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class DefaultTorrentInfoRepository(
    private val localDataSource: ITorrentInfoDataSource,
    private val externalScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ITorrentInfoRepository,DLogger {

    override suspend fun saveTorrentInfo(
        torrentInfo: TorrentInfo,
        torrentPath: String
    ): IResult<Long> {
        printCodeLine()
        val result = localDataSource.getTorrentId(torrentInfo.mInfoHash, torrentPath)
        if (result is IResult.Success) {
            return result
        }
        printCodeLine()

        val deferred = externalScope.async {
            val torrentIdResult =
                localDataSource.saveTorrentInfo(torrentInfo.asTorrentInfoEntity(torrentPath))
            printCodeLine()

            val torrentId = (torrentIdResult as IResult.Success).data
            printCodeLine()

            if (torrentId > 0) {
                saveTorrentFileInfos(torrentInfo, torrentId)
            }
            torrentIdResult
        }

        return deferred.await()
    }

    override suspend fun getTorrentByHash(
        hash: String,
    ): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> {
        return localDataSource.getTorrentByHash(hash)
    }

    override suspend fun getTorrentById(torrentId: Long): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> {
        return localDataSource.getTorrentById(torrentId)
    }

    override suspend fun getTorrentByPath(torrentPath: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> {
       return localDataSource.getTorrentByPath(torrentPath)
    }

    /**
     * 保存TorrentFileInfo
     */
    private suspend fun saveTorrentFileInfos(torrentInfo: TorrentInfo, torrentId: Long) {
        if (torrentInfo.mSubFileInfo != null) {
            for (torrentFileInfo in torrentInfo.mSubFileInfo) {
                val entity = torrentFileInfo.asTorrentFileInfoEntity(torrentId)
                (localDataSource.getTorrentFileInfo(
                    entity.torrentId,
                    entity.realIndex
                ) as? IResult.Error)?.let {
                    localDataSource.saveTorrentFileInfo(entity)
                }
            }
        }
    }

    override fun DLogger.tag(): String {
        return "DefaultTorrentInfoRepository"
    }

}