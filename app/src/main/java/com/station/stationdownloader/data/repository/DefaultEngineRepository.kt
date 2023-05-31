package com.station.stationdownloader.data.repository

import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.IConfigurationRepository
import com.station.stationdownloader.data.datasource.IDownloadTaskRepository
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.local.room.entities.asStationTorrentInfo
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLTaskHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File

class DefaultEngineRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val configurationRepo: IConfigurationRepository,
    private val downloadTaskRepo: IDownloadTaskRepository,
    private val torrentInfoRepo: ITorrentInfoRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IEngineRepository {
    override suspend fun init(): IResult<Unit> {
        return try {
            xlEngine.init()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }
    }

    override suspend fun unInit(): IResult<Unit> {
        return try {
            xlEngine.unInit()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }
    }


    override suspend fun initTask(url: String): IResult<StationDownloadTask> {
        val decodeUrl = TaskTools.getUrlDecodeUrl(url)
        val isSupportNetWork = TaskTools.isSupportNetworkUrl(url)
        if (isSupportNetWork) {
            return initNormalTask(decodeUrl)
        }
        val isTorrent = TaskTools.isTorrentFile(url) && XLTaskHelper.instance()
            .getTorrentInfo(url).mInfoHash != null
        if (!isTorrent) {
            return IResult.Error(
                Exception(TaskExecuteError.ERROR_TORRENT_TASK.name),
                TaskExecuteError.ERROR_TORRENT_TASK.ordinal
            )
        } else {
            val torrentInfo = XLTaskHelper.instance().getTorrentInfo(url)
            torrentInfoRepo.saveTorrentInfo(torrentInfo)

            val taskName = url.substringAfterLast(File.separatorChar)
            val downloadPath =
                File(configurationRepo.getDownloadPath(), taskName.substringAfterLast(".")).path
            val fileCount = torrentInfo.mFileCount
            val stationTorrentInfo = torrentInfo.asStationTorrentInfo()
            return IResult.Success(
                StationDownloadTask(
                    url = decodeUrl,
                    name = taskName,
                    urlType = DownloadUrlType.TORRENT,
                    engine = configurationRepo.getDefaultEngine(),
                    downloadPath = downloadPath,
                    fileCount = fileCount,
                    torrentInfo = stationTorrentInfo
                )
            )
        }
    }

    override fun configure(key: String, values: Array<String>): IResult<Unit> {
        val xlConfigResult = xlEngine.configure(key, values)
        val aria2ConfigResult = aria2Engine.configure(key, values)

        if (xlConfigResult is IResult.Error)
            return xlConfigResult.copy(exception = Exception("[xl] ${xlConfigResult.exception.message}"))

        if (aria2ConfigResult is IResult.Error)
            return aria2ConfigResult.copy(exception = Exception("[aria2] ${aria2ConfigResult.exception.message}"))

        return IResult.Success(Unit)
    }


    private fun initNormalTask(url: String): IResult<StationDownloadTask> {
        val taskName = XLTaskHelper.instance().getFileName(url)
        val downloadPath =
            File(configurationRepo.getDownloadPath(), taskName.substringAfterLast(".")).path
        return IResult.Success(
            StationDownloadTask(
                url = url,
                name = taskName,
                urlType = TaskTools.getUrlType(url),
                engine = configurationRepo.getDefaultEngine(),
                downloadPath = downloadPath,
                fileCount = 1
            )
        )

    }


}