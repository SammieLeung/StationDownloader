package com.station.stationdownloader.data.repository

import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.datasource.IConfigurationRepository
import com.station.stationdownloader.data.datasource.IDownloadTaskRepository
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.station.stationdownloader.data.datasource.engine.ExecuteResult
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.data.datasource.model.StationTorrentInfo
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLTaskHelper
import java.io.File

class DefaultEngineRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val configurationRepo: IConfigurationRepository,
    private val downloadTaskRepo: IDownloadTaskRepository,
    private val torrentInfoRepo:ITorrentInfoRepository
) : IEngineRepository {
    override fun init(): ExecuteResult<Nothing> {
        return try {
            xlEngine.init()
            ExecuteResult.Success
        } catch (e: Exception) {
            ExecuteResult.Error(e)
        }
    }

    override fun unInit(): ExecuteResult<Nothing> {
        return try {
            xlEngine.unInit()
            ExecuteResult.Success
        } catch (e: Exception) {
            ExecuteResult.Error(e)
        }
    }

    override fun initTask(url: String): ExecuteResult<StationDownloadTask> {
        val mUrl = TaskTools.getUrlDecodeUrl(url)
        val isSupportNetWork = TaskTools.isSupportNetworkUrl(url)
        if (isSupportNetWork) {
            val mTaskName = XLTaskHelper.instance().getFileName(mUrl)
            val mDownloadPath =
                File(configurationRepo.getDownloadPath(), mTaskName.substringAfterLast(".")).path
            return ExecuteResult.SuccessResult(
                StationDownloadTask(
                    url = mUrl,
                    name = mTaskName,
                    urlType = TaskTools.getUrlType(mUrl),
                    engine = configurationRepo.getDefaultEngine(),
                    downloadPath = mDownloadPath,
                    fileCount = 1
                )
            )
        }
        var isTorrent = TaskTools.isTorrentFile(url)
        if (!isTorrent) {
            try {
                val torrentInfo = XLTaskHelper.instance().getTorrentInfo(url)
                if (torrentInfo.mInfoHash != null) {
                    isTorrent = true
                } else {
                    return ExecuteResult.Failed(
                        TaskExecuteError.IS_NOT_TORRENT.ordinal,
                        Exception(TaskExecuteError.IS_NOT_TORRENT.name)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return ExecuteResult.Failed(
                    TaskExecuteError.IS_NOT_TORRENT.ordinal,
                    Exception(TaskExecuteError.IS_NOT_TORRENT.name)
                )
            }
        }

        if (isTorrent) {
            val torrentInfo = XLTaskHelper.instance().getTorrentInfo(url)
            if (torrentInfo.mInfoHash == null) {
                return ExecuteResult.Failed(
                    TaskExecuteError.IS_NOT_TORRENT.ordinal,
                    Exception(TaskExecuteError.IS_NOT_TORRENT.name)
                )
            }
            val mTaskName = url.substringAfterLast(File.separatorChar)
            val mDownloadPath =
                File(configurationRepo.getDownloadPath(), mTaskName.substringAfterLast(".")).path
            val fileCount = torrentInfo.mFileCount

            if (torrentInfo.mIsMultiFiles) {

            } else {

            }

            return ExecuteResult.SuccessResult(
                StationDownloadTask(
                    url = mUrl,
                    name = mTaskName,
                    urlType = DownloadUrlType.TORRENT,
                    engine = configurationRepo.getDefaultEngine(),
                    downloadPath = mDownloadPath,


                    )
            )
        }

        return ExecuteResult.Failed(
            TaskExecuteError.NOT_SUPPORT_URL.ordinal,
            Exception(TaskExecuteError.NOT_SUPPORT_URL.name)
        )
    }

    override fun configure(key: String, values: Array<String>): ExecuteResult<Nothing> {
        val xlConfigResult = xlEngine.configure(key, values)
        val aria2ConfigResult = aria2Engine.configure(key, values)

        if (xlConfigResult is ExecuteResult.Failed)
            return xlConfigResult.copy(error = Exception("[xl] ${xlConfigResult.error.message}"))

        if (aria2ConfigResult is ExecuteResult.Failed)
            return aria2ConfigResult.copy(error = Exception("[aria2] ${aria2ConfigResult.error.message}"))

        return ExecuteResult.Success
    }

    private fun getTorrentTaskName(url: String): String {
        val torrentInfo = XLTaskHelper.instance().getTorrentInfo(url)
        var taskName = torrentInfo.mMultiFileBaseFolder
        if (taskName.isEmpty() == true)
            return url
        return taskName
    }


}