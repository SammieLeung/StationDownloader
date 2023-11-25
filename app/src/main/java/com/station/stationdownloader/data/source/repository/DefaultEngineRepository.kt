package com.station.stationdownloader.data.source.repository

import com.orhanobut.logger.Logger
import com.station.stationdownloader.Aria2TorrentTask
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.TaskId
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.contants.Aria2Options
import com.station.stationdownloader.contants.CommonOptions
import com.station.stationdownloader.contants.Options
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.contants.XLOptions
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.isFailed
import com.station.stationdownloader.data.result
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.aria2.Aria2Engine
import com.station.stationdownloader.data.source.local.engine.aria2.connection.client.WebSocketClient
import com.station.stationdownloader.data.source.local.engine.aria2.connection.exception.Aria2Exception
import com.station.stationdownloader.data.source.local.engine.xl.XLEngine
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.asXLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.data.succeeded
import com.station.stationdownloader.utils.DLogger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DefaultEngineRepository(
    private val xlEngine: XLEngine,
    private val aria2Engine: Aria2Engine,
    private val taskRepo: IDownloadTaskRepository,
    private val configRepo: DefaultConfigurationRepository,
    private val torrentInfoRepo: ITorrentInfoRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : DLogger {
    private var maxThreadCount = AtomicInteger(0)
    private val downloadingTaskList: MutableList<String> = mutableListOf()

    fun initEngines(): Flow<Pair<DownloadEngine, IResult<String>>> =
        flow {
            emit(Pair(DownloadEngine.XL, xlEngine.init()))
            emit(Pair(DownloadEngine.ARIA2, aria2Engine.init()))
        }

    suspend fun initEngine(engine: DownloadEngine): Flow<Pair<DownloadEngine, IResult<String>>> =
        flow {
            when (engine) {
                DownloadEngine.XL -> {
                    emit(Pair(DownloadEngine.XL, xlEngine.init()))
                }

                DownloadEngine.ARIA2 -> {
                    emit(Pair(DownloadEngine.ARIA2, aria2Engine.init()))
                }

                DownloadEngine.INVALID_ENGINE -> {
                    emit(
                        Pair(
                            DownloadEngine.INVALID_ENGINE, IResult.Error(
                                Exception(TaskExecuteError.INVALID_ENGINE_TYPE.name),
                                TaskExecuteError.INVALID_ENGINE_TYPE.ordinal
                            )
                        )
                    )
                }
            }
        }


    suspend fun unInitEngines(): IResult<Unit> {
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
            val maxThread = configRepo.getValue(CommonOptions.MaxThread).toInt()
            var downloadStatus = DownloadTaskStatus.DOWNLOADING


            return@withContext when (stationDownloadTask.engine) {
                DownloadEngine.XL -> {
                    if (maxThreadCount.get() >= maxThread) {
                        return@withContext IResult.Error(
                            Exception("max thread count is $maxThread"),
                            TaskExecuteError.TASK_NUMBER_REACHED_LIMIT.ordinal
                        )
                    }
                    val taskIdResult = xlEngine.startTask(
                        realUrl, downloadPath, name, urlType, fileCount, selectIndexes
                    )

                    if (taskIdResult is IResult.Error) {
                        return@withContext taskIdResult
                    }
                    taskIdResult as IResult.Success
                    val taskId = taskIdResult.data
                    if (taskId.toLong() <= 0L) {
                        downloadStatus = DownloadTaskStatus.FAILED
                    }
                    taskRepo.updateTask(
                        stationDownloadTask.copy(status = downloadStatus).asXLDownloadTaskEntity()
                    )
                    maxThreadCount.incrementAndGet()
                    IResult.Success(
                        TaskId(DownloadEngine.XL, taskId)
                    )
                }

                DownloadEngine.ARIA2 -> {
                    if (maxThreadCount.get() >= maxThread) {
                        if (!aria2Engine.containGid(realUrl)) {
                            val pauseTaskResponse = aria2Engine.addPauseTask(
                                realUrl,
                                downloadPath,
                                urlType,
                                selectIndexes
                            )
                            if (pauseTaskResponse is IResult.Error) {
                                return@withContext pauseTaskResponse
                            }

                            return@withContext IResult.Success(
                                TaskId(
                                    DownloadEngine.ARIA2,
                                    pauseTaskResponse.result()
                                )
                            )
                        } else {
                            return@withContext IResult.Success(
                                TaskId(
                                    DownloadEngine.ARIA2,
                                    aria2Engine.getGid(realUrl)
                                )
                            )
                        }
//                        return@withContext IResult.Error(
//                            Exception("max thread count is $maxThread"),
//                            TaskExecuteError.TASK_NUMBER_REACHED_LIMIT.ordinal
//                        )
                    }

                    val startTaskResult = aria2Engine.startTask(
                        realUrl, downloadPath, name, urlType, fileCount, selectIndexes
                    )
                    if (startTaskResult is IResult.Error) {
//                        maxThreadCount.decrementAndGet()
                        Logger.e("${startTaskResult.exception}")
                        return@withContext startTaskResult
                    }
                    startTaskResult as IResult.Success
                    val taskId = startTaskResult.data
                    taskRepo.updateTask(
                        stationDownloadTask.copy(status = downloadStatus).asXLDownloadTaskEntity()
                    )
                    maxThreadCount.incrementAndGet()
                    IResult.Success(
                        TaskId(DownloadEngine.ARIA2, taskId)
                    )
                }

                DownloadEngine.INVALID_ENGINE -> {
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
                taskRepo.updateTask(
                    stationDownloadTask.copy(
                        status = taskStatus,
                        downloadSize = taskInfo.mDownloadSize,
                        totalSize = taskInfo.mFileSize
                    ).asXLDownloadTaskEntity()
                )
            }

            DownloadEngine.ARIA2 -> {

                val ariaTaskStatus =
                    aria2Engine.tellStatus(taskId.id, url = stationDownloadTask.url)
                if (ariaTaskStatus is IResult.Error) {
                    Logger.e("${ariaTaskStatus.exception}")
                    taskRepo.updateTask(
                        stationDownloadTask.copy(
                            status = DownloadTaskStatus.PAUSE,
                        ).asXLDownloadTaskEntity()
                    )
                    val stopResponse = aria2Engine.stopTask(taskId.id)
                    if (stopResponse.succeeded) {
                        if (maxThreadCount.get() > 0) maxThreadCount.decrementAndGet()
                    }
                } else {
                    val taskInfo = (ariaTaskStatus as IResult.Success).data
                    val taskStatus = when (taskInfo.status) {
                        ITaskState.DONE.code -> DownloadTaskStatus.COMPLETED
                        else -> DownloadTaskStatus.PAUSE
                    }
                    taskRepo.updateTask(
                        stationDownloadTask.copy(
                            status = taskStatus,
                            downloadSize = taskInfo.downloadSize,
                            totalSize = taskInfo.totalSize
                        ).asXLDownloadTaskEntity()
                    )
                    if (taskStatus == DownloadTaskStatus.COMPLETED) {
                        if (maxThreadCount.get() > 0) maxThreadCount.decrementAndGet()
                        aria2Engine.removeDownloadResult(taskId.id)
                    } else {
                        val stopResponse = aria2Engine.stopTask(taskId.id)
                        if (stopResponse.succeeded) {
                            if (maxThreadCount.get() > 0) maxThreadCount.decrementAndGet()
                        }
                    }
                }
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
                val newTask = taskRepo.getTaskByUrl(stationDownloadTask.url)
                    ?: return@withContext IResult.Error(Exception("task not found"))
                return@withContext startTask(newTask.asStationDownloadTask())
            }

            DownloadEngine.ARIA2 -> TODO()
            DownloadEngine.INVALID_ENGINE -> TODO()
        }

    }

    suspend fun removeAria2Task(realUrl: String): IResult<Boolean> {
        val removeTaskResponse = aria2Engine.removeTask(realUrl)
        if (removeTaskResponse.isFailed) {
            return removeTaskResponse
        }
        return removeTaskResponse
    }

    suspend fun getAria2TaskStatusByUrl(url: String): IResult<TaskStatus> {
        val entity = taskRepo.getTaskByUrl(url)
            ?: return IResult.Error(
                Exception(TaskExecuteError.TASK_NOT_FOUND.name),
                TaskExecuteError.TASK_NOT_FOUND.ordinal
            )
        return aria2Engine.tellStatusByUrl(url,entity.realUrl )
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

    suspend fun saveSession(): IResult<Boolean> {
        return aria2Engine.saveSession()
    }

    suspend fun changeOption(option: Options, value: String) {
        when (option) {
            is CommonOptions -> {
                configRepo.setValue(option, value)
            }

            is Aria2Options -> {
                aria2Engine.setOptions(option, value)
            }

            is XLOptions -> {
                xlEngine.setOptions(option, value)
            }
        }
    }

    fun setAria2NotifyListener(listener: WebSocketClient.OnNotify?) {
        aria2Engine.setNotifyListener(listener)
    }

    suspend fun isEngineInitialized(engine: DownloadEngine): Boolean {
        return when (engine) {
            DownloadEngine.XL -> {
                xlEngine.isInit()
            }

            DownloadEngine.ARIA2 -> {
                aria2Engine.isInit()
            }

            DownloadEngine.INVALID_ENGINE -> {
                false
            }
        }
    }

    suspend fun isEnginesInitialized(): Boolean {
        return xlEngine.isInit() && aria2Engine.isInit()
    }

    override fun DLogger.tag(): String {
        return "DefaultEngineRepository"
    }

}