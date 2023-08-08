package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.contants.ConfigureError
import com.station.stationdownloader.contants.DOWNLOAD_ENGINE
import com.station.stationdownloader.contants.DOWNLOAD_PATH
import com.station.stationdownloader.contants.MAX_THREAD
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.asXLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.xunlei.downloadlib.XLDownloadManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DefaultEngineRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val downloadTaskRepo: IDownloadTaskRepository,
    private val configurationDataSource: IConfigurationDataSource,
    private val configRepo: IConfigurationRepository,
    private val torrentInfoRepo: ITorrentInfoRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,

    ) : IEngineRepository {
    private var maxThread: Int = 0
    private var maxThreadCount = AtomicInteger(0)

    override suspend fun init(): IResult<Unit> {
        return try {
            xlEngine.init()
            aria2Engine.init()
            loadConfig()
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

    override suspend fun initUrl(url: String): IResult<NewTaskConfigModel> {
        return xlEngine.initUrl(url)
    }

    override suspend fun getTorrentInfo(torrentPath: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
        withContext(defaultDispatcher) {
            if (torrentPath.isEmpty()) {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.NOT_SUPPORT_URL.name),
                    TaskExecuteError.NOT_SUPPORT_URL.ordinal
                )
            }
            if (!File(torrentPath).exists()) {
                return@withContext IResult.Error(
                    Exception(TaskExecuteError.TORRENT_FILE_NOT_FOUND.name),
                    TaskExecuteError.TORRENT_FILE_NOT_FOUND.ordinal
                )
            }
            xlEngine as XLEngine
            val torrentInfoEntityResult = torrentInfoRepo.getTorrentByPath(torrentPath)
            if (torrentInfoEntityResult is IResult.Success) {
                val torrentInfoMap = torrentInfoEntityResult.data
                if (torrentInfoMap.isNotEmpty()) {
                    return@withContext IResult.Success(torrentInfoMap)
                }
            }

            val torrentInfoResult = xlEngine.getTorrentInfo(torrentPath)
            if (torrentInfoResult is IResult.Error) {
                return@withContext torrentInfoResult
            }

            val torrentInfo = (torrentInfoResult as IResult.Success).data
            val torrentIdResult =
                torrentInfoRepo.saveTorrentInfo(
                    torrentInfo = torrentInfo,
                    torrentPath = torrentPath
                )
            if (torrentIdResult is IResult.Error) {
                return@withContext torrentIdResult
            }

            val torrentId = (torrentIdResult as IResult.Success).data
            return@withContext torrentInfoRepo.getTorrentById(torrentId)
        }

    override suspend fun startTask(stationDownloadTask: StationDownloadTask): IResult<Long> =
        withContext(defaultDispatcher) {
            val url: String = stationDownloadTask.realUrl
            val downloadPath: String = stationDownloadTask.downloadPath
            val name: String = stationDownloadTask.name
            val urlType: DownloadUrlType = stationDownloadTask.urlType
            val fileCount: Int = stationDownloadTask.fileCount
            val selectIndexes: IntArray = stationDownloadTask.selectIndexes.toIntArray()
            if (maxThreadCount.get() == maxThread) {
                return@withContext IResult.Error(
                    Exception("max thread count is $maxThread"),
                    TaskExecuteError.TASK_NUMBER_REACHED_LIMIT.ordinal
                )
            }
            maxThreadCount.incrementAndGet()
            return@withContext when (stationDownloadTask.engine) {
                DownloadEngine.XL -> {
                    val taskIdResult = xlEngine.startTask(
                        url, downloadPath, name, urlType, fileCount, selectIndexes
                    )
                    var downloadStatus = DownloadTaskStatus.DOWNLOADING
                    if (taskIdResult is IResult.Error) {
                        maxThreadCount.decrementAndGet()
                        downloadStatus = DownloadTaskStatus.PAUSE
                    }
                    taskIdResult as IResult.Success
                    if (taskIdResult.data <= 0L)
                        maxThreadCount.decrementAndGet()
                    downloadTaskRepo.updateTask(
                        stationDownloadTask.copy(status = downloadStatus).asXLDownloadTaskEntity()
                    )
                    taskIdResult
                }

                DownloadEngine.ARIA2 -> {
                    val startTaskResult = aria2Engine.startTask(
                        url, downloadPath, name, urlType, fileCount, selectIndexes
                    )
                    startTaskResult
                }
            }
        }

    override suspend fun stopTask(currentTaskId: Long, stationDownloadTask: StationDownloadTask) {
        xlEngine as XLEngine
        val taskInfo = xlEngine.getTaskInfo(currentTaskId)
        if (taskInfo.mTaskId == 0L) return
        val taskStatus = when (taskInfo.mTaskStatus) {
            ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
            else -> DownloadTaskStatus.PAUSE
        }
        xlEngine.stopTask(currentTaskId)
        if (maxThreadCount.get() > 0) maxThreadCount.decrementAndGet()
        downloadTaskRepo.updateTask(
            stationDownloadTask.copy(
                status = taskStatus,
                downloadSize = taskInfo.mDownloadSize,
                totalSize = taskInfo.mFileSize
            ).asXLDownloadTaskEntity()
        )
    }

    override suspend fun restartTask(
        currentTaskId: Long, stationDownloadTask: StationDownloadTask
    ): IResult<Long> = withContext(defaultDispatcher)
    {
        stopTask(currentTaskId, stationDownloadTask)
        val newTask = downloadTaskRepo.getTaskByUrl(stationDownloadTask.url)
            ?: return@withContext IResult.Error(Exception("task not found"))
        return@withContext startTask(newTask.asStationDownloadTask())
    }

    private suspend fun loadConfig(): IResult<Boolean> {
        val speedLimit = configurationDataSource.getSpeedLimit()
        maxThread = configurationDataSource.getMaxThread()
        val downloadPath = configurationDataSource.getDownloadPath()
        val defaultEngine = configurationDataSource.getDefaultEngine()
        val xlConfigResult = xlEngine.configure(SPEED_LIMIT, speedLimit.toString())
        val aria2ConfigResult = aria2Engine.configure(SPEED_LIMIT, speedLimit.toString())

        if (xlConfigResult is IResult.Error) return xlConfigResult.copy(exception = Exception("[xl] ${xlConfigResult.exception.message}"))
        if (aria2ConfigResult is IResult.Error) return aria2ConfigResult.copy(
            exception = Exception(
                "[aria2] ${aria2ConfigResult.exception.message}"
            )
        )
        return IResult.Success(true)
    }


    override suspend fun configure(key: String, value: String): IResult<Unit> {
        when (key) {
            SPEED_LIMIT -> {
                XLDownloadManager.getInstance().setSpeedLimit(value.toLong(), value.toLong())
                configurationDataSource.setSpeedLimit(value.toLong())
            }

            MAX_THREAD -> {
                maxThread = value.toInt()
                configurationDataSource.setMaxThread(value.toInt())
            }

            DOWNLOAD_ENGINE -> {
                configurationDataSource.setDefaultEngine(DownloadEngine.valueOf(value))
            }

            DOWNLOAD_PATH -> {
                configurationDataSource.setDownloadPath(value)
            }

            else -> return IResult.Error(
                Exception(ConfigureError.NOT_SUPPORT_CONFIGURATION.name),
                ConfigureError.NOT_SUPPORT_CONFIGURATION.ordinal
            )
        }
        val xlConfigResult = xlEngine.configure(key, value)
        val aria2ConfigResult = aria2Engine.configure(key, value)

        if (xlConfigResult is IResult.Error) return xlConfigResult.copy(exception = Exception("[xl] ${xlConfigResult.exception.message}"))
        if (aria2ConfigResult is IResult.Error) return aria2ConfigResult.copy(
            exception = Exception(
                "[aria2] ${aria2ConfigResult.exception.message}"
            )
        )
        return IResult.Success(Unit)
    }


}