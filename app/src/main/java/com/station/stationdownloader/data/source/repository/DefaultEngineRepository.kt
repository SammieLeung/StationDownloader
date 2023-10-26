package com.station.stationdownloader.data.source.repository

import com.orhanobut.logger.Logger
import com.station.stationdownloader.Aria2TorrentTask
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.TaskId
import com.station.stationdownloader.TaskStatus
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
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.EngineStatus
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.aria2.Aria2Engine
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.asXLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.xunlei.downloadlib.XLDownloadManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DefaultEngineRepository(
    private val xlEngine: XLEngine,
    private val aria2Engine: Aria2Engine,
    private val downloadTaskRepo: IDownloadTaskRepository,
    private val configurationDataSource: IConfigurationDataSource,
    private val configRepo: IConfigurationRepository,
    private val torrentInfoRepo: ITorrentInfoRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private var maxThread: Int = 0
    private var maxThreadCount = AtomicInteger(0)

    fun init(): Flow<Pair<DownloadEngine, IResult<String>>> =
        flow {
            emit(Pair(DownloadEngine.XL, xlEngine.init()))
            emit(Pair(DownloadEngine.ARIA2, aria2Engine.init()))
        }.onCompletion {
            val configResult = (loadLocalConfigurations() as IResult.Success).data
            Logger.d("${DownloadEngine.XL.name} ${configResult[DownloadEngine.XL.name]}")
            Logger.d("${DownloadEngine.ARIA2.name} ${configResult[DownloadEngine.ARIA2.name]}")
        }

    suspend fun loadLocalConfigurations(): IResult<Map<String, IResult<String>>> {
        maxThread = configurationDataSource.getMaxThread()
        val xlResult = loadXLConfig()
        val aria2Result = loadAria2Config()
        return IResult.Success(
            mapOf(
                DownloadEngine.XL.name to xlResult,
                DownloadEngine.ARIA2.name to aria2Result
            )
        )
    }

    suspend fun unInit(): IResult<Unit> {
        return try {
            xlEngine.unInit()
            aria2Engine.unInit()
            IResult.Success(Unit)
        } catch (e: Exception) {
            IResult.Error(exception = e)
        }
    }

    suspend fun initUrl(url: String): IResult<NewTaskConfigModel> {
        return xlEngine.initUrl(url)
    }

    suspend fun getTorrentInfo(torrentPath: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>> =
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
            xlEngine
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

    suspend fun startTask(stationDownloadTask: StationDownloadTask): IResult<TaskId> =
        withContext(defaultDispatcher) {
            val realUrl: String = stationDownloadTask.realUrl
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
            var downloadStatus = DownloadTaskStatus.DOWNLOADING
            return@withContext when (stationDownloadTask.engine) {
                DownloadEngine.XL -> {
                    val taskIdResult = xlEngine.startTask(
                        realUrl, downloadPath, name, urlType, fileCount, selectIndexes
                    )

                    if (taskIdResult is IResult.Error) {
                        maxThreadCount.decrementAndGet()
                        return@withContext taskIdResult
                    }
                    taskIdResult as IResult.Success
                    val taskId = taskIdResult.data
                    if (taskId.toLong() <= 0L) {
                        maxThreadCount.decrementAndGet()
                        downloadStatus = DownloadTaskStatus.FAILED
                    }
                    downloadTaskRepo.updateTask(
                        stationDownloadTask.copy(status = downloadStatus).asXLDownloadTaskEntity()
                    )
                    IResult.Success(
                        TaskId(DownloadEngine.XL, taskId)
                    )
                }

                DownloadEngine.ARIA2 -> {
                    val startTaskResult = aria2Engine.startTask(
                        realUrl, downloadPath, name, urlType, fileCount, selectIndexes
                    )
                    if (startTaskResult is IResult.Error) {
                        maxThreadCount.decrementAndGet()
                        startTaskResult.exception.printStackTrace()
                        return@withContext startTaskResult
                    }
                    startTaskResult as IResult.Success
                    val taskId = startTaskResult.data
                    downloadTaskRepo.updateTask(
                        stationDownloadTask.copy(status = downloadStatus).asXLDownloadTaskEntity()
                    )
                    IResult.Success(
                        TaskId(DownloadEngine.ARIA2, taskId)
                    )
                }

                DownloadEngine.INVALID_ENGINE -> {
                    maxThreadCount.decrementAndGet()
                    IResult.Error(
                        Exception(TaskExecuteError.INVALID_ENGINE_TYPE.name),
                        TaskExecuteError.INVALID_ENGINE_TYPE.ordinal
                    )
                }
            }
        }

    suspend fun stopTask(
        taskId: TaskId,
        stationDownloadTask: StationDownloadTask
    ) {
        when (taskId.engine) {
            DownloadEngine.XL -> {
                val currentTaskId = taskId.id.toLong()
                val taskInfo = xlEngine.getTaskInfo(currentTaskId)
                if (taskInfo.mTaskId == 0L) return
                val taskStatus = when (taskInfo.mTaskStatus) {
                    ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
                    else -> DownloadTaskStatus.PAUSE
                }
                xlEngine.stopTask(taskId.id)
                if (maxThreadCount.get() > 0) maxThreadCount.decrementAndGet()
                downloadTaskRepo.updateTask(
                    stationDownloadTask.copy(
                        status = taskStatus,
                        downloadSize = taskInfo.mDownloadSize,
                        totalSize = taskInfo.mFileSize
                    ).asXLDownloadTaskEntity()
                )
            }

            DownloadEngine.ARIA2 -> {
                aria2Engine.stopTask(taskId.id)
                if (maxThreadCount.get() > 0) maxThreadCount.decrementAndGet()
                val ariaTaskStatus =
                    aria2Engine.tellStatus(taskId.id, url = stationDownloadTask.url)
                if (ariaTaskStatus is IResult.Error) {
                    Logger.e(ariaTaskStatus.exception.message.toString())
                }
                val taskInfo = (ariaTaskStatus as IResult.Success).data
                val taskStatus = when (taskInfo.status) {
                    ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
                    else -> DownloadTaskStatus.PAUSE
                }
                downloadTaskRepo.updateTask(
                    stationDownloadTask.copy(
                        status = taskStatus,
                        downloadSize = taskInfo.downloadSize,
                        totalSize = taskInfo.totalSize
                    ).asXLDownloadTaskEntity()
                )
            }

            DownloadEngine.INVALID_ENGINE -> {

            }
        }

    }

    suspend fun restartTask(
        taskId: TaskId,
        stationDownloadTask: StationDownloadTask
    ): IResult<TaskId> = withContext(defaultDispatcher)
    {
        when (taskId.engine) {
            DownloadEngine.XL -> {
                stopTask(taskId, stationDownloadTask)
                val newTask = downloadTaskRepo.getTaskByUrl(stationDownloadTask.url)
                    ?: return@withContext IResult.Error(Exception("task not found"))
                return@withContext startTask(newTask.asStationDownloadTask())
            }

            DownloadEngine.ARIA2 -> TODO()
            DownloadEngine.INVALID_ENGINE -> TODO()
        }

    }


    suspend fun removeAria2Task(url: String): IResult<Boolean> {
        val task = downloadTaskRepo.getTaskByUrl(url) ?: return IResult.Error(
            Exception(
                TaskExecuteError.TASK_NOT_FOUND.name
            ), TaskExecuteError.TASK_NOT_FOUND.ordinal
        )
        return aria2Engine.removeTask(task.realUrl)

    }

    suspend fun getAria2TaskStatus(gid: String, url: String): IResult<TaskStatus> {
        return aria2Engine.tellStatus(gid, url)
    }

    suspend fun tellAll(): List<Aria2TorrentTask> = withContext(defaultDispatcher) {
        val list = mutableListOf<Aria2TorrentTask>()
        val activeTaskList = aria2Engine.tellActive()
        val waitingTaskList = aria2Engine.tellWaiting(0, Int.MAX_VALUE)
        val stoppedTaskList = aria2Engine.tellStopped(0, Int.MAX_VALUE)
        list.addAll(activeTaskList)
        list.addAll(waitingTaskList)
        list.addAll(stoppedTaskList)
        return@withContext list
    }

    private suspend fun formatTorrentList(torrentTaskList: List<Aria2TorrentTask>): List<Aria2TorrentTask> {
        val list = mutableListOf<Aria2TorrentTask>()
        torrentTaskList.forEach {
            val info = it.hashInfo
            val status = it.taskStatus
            val xlEntity = downloadTaskRepo.getTorrentTaskByHash(info)
            if (xlEntity != null) {
                list.add(
                    it.copy(
                        taskStatus = status.copy(
                            url = xlEntity.url
                        )
                    )
                )
            }
        }
        return list
    }

    private suspend fun loadXLConfig(): IResult<String> {
        val speedLimit = configurationDataSource.getSpeedLimit()
        val xlConfigResult = xlEngine.configure(SPEED_LIMIT, speedLimit.toString())
        if (xlConfigResult is IResult.Error)
            return xlConfigResult.copy(exception = Exception("[${DownloadEngine.XL}] ${xlConfigResult.exception.message}"))
        return xlConfigResult
    }

    private suspend fun loadAria2Config(): IResult<String> {
        val speedLimit = configurationDataSource.getSpeedLimit()
        val aria2ConfigResult = aria2Engine.configure(SPEED_LIMIT, speedLimit.toString())
        if (aria2ConfigResult is IResult.Error)
            return aria2ConfigResult.copy(exception = Exception("[${DownloadEngine.ARIA2}] ${aria2ConfigResult.exception.message}"))
        return aria2ConfigResult
    }

    suspend fun saveSession(): IResult<Boolean> {
        return aria2Engine.saveSession()
    }

    suspend fun configure(key: String, value: String): IResult<Unit> {
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

    suspend fun getEngineStatus(): Map<DownloadEngine, EngineStatus> {
        val xlStatus = xlEngine.getEngineStatus()
        val aria2Status = aria2Engine.getEngineStatus()
        return mapOf(
            DownloadEngine.XL to xlStatus,
            DownloadEngine.ARIA2 to aria2Status
        )
    }


}