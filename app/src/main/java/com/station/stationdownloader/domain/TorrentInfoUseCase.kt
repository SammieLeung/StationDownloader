package com.station.stationdownloader.domain

import android.view.KeyEvent.DispatcherState
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.datasource.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.datasource.model.StationTorrentFileInfo
import com.station.stationdownloader.data.datasource.model.StationTorrentInfo
import com.station.stationdownloader.di.DefaultDispatcher
import com.xunlei.downloadlib.parameter.TorrentInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TorrentInfoUseCase(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {


    suspend operator fun invoke(torrentInfo: TorrentInfo): StationTorrentInfo =
        withContext(defaultDispatcher) {
            StationTorrentInfo(
                fileCount = torrentInfo.mFileCount,
                hash = torrentInfo.mInfoHash,
                isMultiFiles = torrentInfo.mIsMultiFiles,
                multiFileBaseFolder = torrentInfo.mMultiFileBaseFolder,
                subFileInfo = torrentInfo.mSubFileInfo.map {
                    StationTorrentFileInfo(
                        fileIndex = it.mFileIndex,
                        fileName = it.mFileName,
                        fileSize = it.mFileSize,
                        realIndex = it.mRealIndex,
                        subPath = it.mSubPath,
                    )
                }
            )
        }

    suspend operator fun invoke(map: Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>): StationTorrentInfo =
        withContext(defaultDispatcher) {
            if (map.isEmpty() || map.size > 1) {
                throw DuplicateException("map size is duplicate!")
            }
            val torrentInfoEntity: TorrentInfoEntity = map.keys.first()
            val torrentFileInfoEntityList = map.values.first()
            StationTorrentInfo(
                fileCount = torrentInfoEntity.fileCount,
                hash = torrentInfoEntity.hash,
                isMultiFiles = torrentInfoEntity.isMultiFiles,
                multiFileBaseFolder = torrentInfoEntity.multiFileBaseFolder,
                subFileInfo = torrentFileInfoEntityList.map {
                    StationTorrentFileInfo(
                        fileIndex = it.fileIndex,
                        fileName = it.fileName,
                        fileSize = it.fileSize,
                        realIndex = it.realIndex,
                        subPath = it.subPath
                    )
                }
            )
        }

    suspend operator fun invoke(stationTorrentInfo: StationTorrentInfo): Map<TorrentInfoEntity, List<TorrentFileInfoEntity>> =
        withContext(defaultDispatcher) {
            withContext(ioDispatcher) {

            }
            return@withContext mapOf()
        }
}

class DuplicateException(message: String) : Exception(message)