package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.isFailed
import com.station.stationdownloader.data.result
import com.station.stationdownloader.data.source.ITorrentInfoDataSource
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asTorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asTorrentInfoEntity
import com.station.stationdownloader.data.succeeded
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
) : ITorrentInfoRepository, DLogger {

    override suspend fun saveTorrentInfo(
        torrentInfo: TorrentInfo,
        torrentPath: String
    ): IResult<Long> {
        val result = localDataSource.getTorrentInfoByHash(torrentInfo.mInfoHash)
        if (result.succeeded) {
            if (result.result().torrentPath == torrentPath) {
                return IResult.Success(result.result().id)
            } else {
                val updateResponse = localDataSource.updateTorrentInfo(
                    result.result().copy(torrentPath = torrentPath)
                )
                return if (updateResponse.isFailed) {
                    return updateResponse as IResult.Error
                } else {
                    IResult.Success(result.result().id)
                }
            }
        }

        val deferred = externalScope.async {
            val torrentIdResult =
                localDataSource.saveTorrentInfo(torrentInfo.asTorrentInfoEntity(torrentPath))

            val torrentId = (torrentIdResult as IResult.Success).data
            if (torrentId > 0) {
                saveTorrentFileInfos(torrentInfo, torrentId)
            }
            torrentIdResult
        }

        return deferred.await()
    }

    override suspend fun getTorrentInfoById(torrentId: Long): IResult<TorrentInfoEntity> {
        return localDataSource.getTorrentInfoById(torrentId)
    }

    override suspend fun getTorrentByHash(
        hash: String,
    ): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> {
        return localDataSource.getTorrentByHash(hash)
    }

    override suspend fun getTorrentHash(torrentId: Long): IResult<String> {
        val torrentBaseInfoResponse =  localDataSource.getTorrentInfoById(torrentId)
        if(torrentBaseInfoResponse.succeeded){
            return IResult.Success(torrentBaseInfoResponse.result().hash)
        }
        return torrentBaseInfoResponse as IResult.Error
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