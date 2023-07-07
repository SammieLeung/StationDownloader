package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.room.entities.asXLTorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asXLTorrentInfoEntity
import com.xunlei.downloadlib.parameter.TorrentInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class DefaultTorrentInfoRepository(
    private val localDataSource: ITorrentInfoDataSource,
    private val externalScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ITorrentInfoRepository {

    override suspend fun saveTorrentInfo(torrentInfo: TorrentInfo): Long {
        return when (
            val result = localDataSource.getTorrentId(torrentInfo.mInfoHash)
        ) {
            is IResult.Success -> {
                result.data
            }

            is IResult.Error -> {
                val result = externalScope.async {
                    val torrentId =
                        localDataSource.saveTorrentInfo(torrentInfo.asXLTorrentInfoEntity())
                    if (torrentId > 0) {
                        saveTorrentFileInfos(torrentInfo, torrentId)
                    }
                    torrentId
                }

                return result.await()
            }
        }
    }

    /**
     * 保存TorrentFileInfo
     */
    private suspend fun saveTorrentFileInfos(torrentInfo: TorrentInfo, torrentId: Long) {
        if (torrentInfo.mSubFileInfo != null) {
            for (torrentFileInfo in torrentInfo.mSubFileInfo) {
                val entity = torrentFileInfo.asXLTorrentFileInfoEntity(torrentId)
                (localDataSource.getTorrentFileInfo(
                    entity.torrentId,
                    entity.realIndex
                ) as? IResult.Error)?.let {
                    localDataSource.saveTorrentFileInfo(entity)
                }
            }
        }
    }

}