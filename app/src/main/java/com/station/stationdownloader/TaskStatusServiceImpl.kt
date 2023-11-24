package com.station.stationdownloader

import android.content.ContentValues
import android.net.Uri
import com.orhanobut.logger.Logger
import com.station.stationdownloader.TaskId.Companion.INVALID_ID
import com.station.stationdownloader.contants.Aria2Options
import com.station.stationdownloader.contants.CommonOptions
import com.station.stationdownloader.contants.FAILED
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.contants.XLOptions
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.isFailed
import com.station.stationdownloader.data.result
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.getCheckedFilePaths
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.data.source.local.room.entities.asRemoteTorrentInfo
import com.station.stationdownloader.data.source.remote.json.RemoteDeviceStorage
import com.station.stationdownloader.data.source.remote.json.RemoteGetDownloadConfig
import com.station.stationdownloader.data.source.remote.json.RemoteSetDownloadConfig
import com.station.stationdownloader.data.source.remote.json.RemoteStartTask
import com.station.stationdownloader.data.source.remote.json.RemoteTask
import com.station.stationdownloader.data.source.remote.json.RemoteTaskStatus
import com.station.stationdownloader.data.source.repository.DefaultConfigurationRepository
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.utils.DLogger
import com.station.stationkitkt.MoshiHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class TaskStatusServiceImpl(
    private val service: TaskService,
    private val serviceScope: CoroutineScope
) : ITaskStatusService.Stub(), DLogger {
    private val entryPoint: TaskStatusServiceEntryPoint
    private val serviceListenerList = mutableListOf<TaskServiceListenerWrapper>()

    init {
        entryPoint = EntryPointAccessors.fromApplication(
            service.applicationContext,
            TaskStatusServiceEntryPoint::class.java
        )
    }

    private val taskRepo: IDownloadTaskRepository by lazy {
        entryPoint.getTaskRepo()
    }
    private val engineRepo: DefaultEngineRepository by lazy {
        entryPoint.getEngineRepo()
    }
    private val configRepo: DefaultConfigurationRepository by lazy {
        entryPoint.getConfigRepo()
    }
    private val torrentRepo: ITorrentInfoRepository by lazy {
        entryPoint.getTorrentRepo()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskStatusServiceEntryPoint {
        fun getEngineRepo(): DefaultEngineRepository
        fun getTaskRepo(): IDownloadTaskRepository
        fun getConfigRepo(): DefaultConfigurationRepository
        fun getTorrentRepo(): ITorrentInfoRepository
    }

    fun getService(): TaskService = service


    override fun subscribeMovie(
        url: String?,
        path: String?,
        selectIndexes: IntArray?,
        movieId: String?,
        movieType: String?,
        callback: ITaskServiceCallback?
    ) {
        serviceScope.launch {
            handleStartTask(
                url,
                path,
                selectIndexes,
                fun(url: String, downloadPath: String, joinFileString: String) {
                    try {
                        service.contentResolver.insert(
                            Uri.parse("content://com.hphtv.movielibrary.provider.v2/addPoster"),
                            ContentValues().apply {
                                put("download_path", downloadPath)
                                put("file_list", joinFileString)
                                put("movie_id", movieId)
                                put("movie_type", movieType)
                            })
                        service.startTask(url, callback)
                    } catch (e: Exception) {
                        callback?.onFailed(e.message.toString(), FAILED)
                    }
                },
                callback
            )
        }

    }


    override fun startTask(
        url: String?,
        path: String?,
        selectIndexes: IntArray?,
        callback: ITaskServiceCallback?
    ) {
        serviceScope.launch {
            handleStartTask(url, path, selectIndexes, null, callback)
        }
    }

    /*FIXME 先判断任务是否在下载中，如果是，则需要简化处理，直接返回任务id
     *  当前下载逻辑可运行 运行时间223ms
     * 优化后的逻辑运行时间  ?ms
     *  take 1.1 64
     *  take 1.2 ?
     *  take 2.1 249
     *  take 2.2
     *  take 3    1582
     */
    private suspend fun handleStartTask(
        url: String?,
        path: String?,
        selectIndexes: IntArray?,
        predicate: ((String, String, String) -> Unit)?,
        callback: ITaskServiceCallback?
    ) {
        val t = System.currentTimeMillis()
        if (url == null) {
            callback?.onFailed("url is null", TaskExecuteError.NOT_SUPPORT_URL.ordinal)
            return
        }

        taskRepo.getTaskByUrl(url)?.let {
            if (!isTaskConfigurationChange(it, path, selectIndexes)) {
                if (it.status != DownloadTaskStatus.COMPLETED) {
                    val status = service.getDownloadingTaskStatusMap()[url]
                    if (status == null || status.taskId.isInvalid() || status.status == ITaskState.STOP.code) {
                        service.startTask(it.url, callback)
                        logger("take time 1.1 ${System.currentTimeMillis() - t} ms")
                    } else {
                        startTaskSuccess(it, status.taskId.id, callback)
                        logger("take time 1.2 ${System.currentTimeMillis() - t} ms")
                    }

                } else {
                    callback?.onFailed("task is completed", TaskExecuteError.TASK_COMPLETED.ordinal)
                    logger("take time 1.3 ${System.currentTimeMillis() - t} ms")
                }
                Logger.w(service.getString(R.string.repeating_task_nothing_changed) + "=>开始任务命令由远程发起,忽略该问题")
                return
            }
        }

        val newTaskResult = engineRepo.initUrl(url)
        if (newTaskResult is IResult.Error) {
            Logger.e(newTaskResult.exception.message.toString())
            callback?.onFailed(newTaskResult.exception.message, newTaskResult.code)
            return
        }
        newTaskResult as IResult.Success
        var newTaskConfig = newTaskResult.data

        if (selectIndexes == null || selectIndexes.isEmpty()) {
            taskRepo.getTaskByUrl(url)?.takeIf { it.selectIndexes.isNotEmpty() }?.let {
                newTaskConfig.updateSelectIndexes(it.selectIndexes)
            }
        } else {
            newTaskConfig.updateSelectIndexes(selectIndexes.toList())
        }

        path?.let { newTaskConfig = newTaskConfig.update(downloadPath = it) }
        val validateResponse = taskRepo.validateAndPersistTask(newTaskConfig)

        if (validateResponse is IResult.Error) {
            callback?.onFailed(validateResponse.exception.message, validateResponse.code)
            return
        }

        predicate?.let { block ->
            if(validateResponse.result().urlType==DownloadUrlType.TORRENT){
                val torrentInfoResult=torrentRepo.getTorrentInfoById(validateResponse.result().torrentId)
                if(torrentInfoResult.isFailed)
                {
                    torrentInfoResult as IResult.Error
                    callback?.onFailed(torrentInfoResult.exception.message,torrentInfoResult.code)
                    return@let
                }
                var realDownloadDir=File(validateResponse.result().downloadPath)
                if(torrentInfoResult.result().multiFileBaseFolder.isNotEmpty()){
                    realDownloadDir=File(realDownloadDir,torrentInfoResult.result().multiFileBaseFolder)
                }
                block(
                    validateResponse.result().url,
                    realDownloadDir.path,
                    (newTaskConfig._fileTree as TreeNode.Directory).getCheckedFilePaths()
                        .joinToString(";;"),
                )
            }else{
                block(
                    validateResponse.result().url,
                    validateResponse.result().downloadPath,//FIXME 这里使用真实的下载路径，而不是配置的下载路径
                    (newTaskConfig._fileTree as TreeNode.Directory).getCheckedFilePaths()
                        .joinToString(";;"),
                )
            }
        } ?: {
            service.startTask(validateResponse.result().url, callback)
            logger("take time 3 ${System.currentTimeMillis() - t} ms")
        }

    }

    private suspend fun startTaskSuccess(
        taskEntity: XLDownloadTaskEntity,
        taskId: String,
        callback: ITaskServiceCallback?
    ) {
        val remoteStartTask = RemoteStartTask.Create(taskEntity, taskId, torrentRepo)
        callback?.onResult(MoshiHelper.toJson(remoteStartTask))
    }

    /**
     * 判断任务配置是否发生变化
     * @param entity XLDownloadTaskEntity
     * @param path String?
     * @param selectIndexes IntArray?
     */
    private fun isTaskConfigurationChange(
        entity: XLDownloadTaskEntity,
        downloadPath: String?,
        selectIndexes: IntArray?
    ): Boolean {
        var isChange = false
        downloadPath?.let {
            val taskName = entity.name
            isChange = entity.downloadPath != File(it, taskName).path
        }
        selectIndexes?.let {
            isChange = isChange || entity.selectIndexes != selectIndexes.toList()
        }
        return isChange
    }

    override fun stopTask(url: String?, callback: ITaskServiceCallback?) {
        serviceScope.launch {
            url?.let {
                service.stopTask(it)
            }
        }
    }

    override fun startAllTasks(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun stopAllTask(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun getDownloadPath(callback: ITaskServiceCallback?) {
        serviceScope.launch {
            val downloadPath = configRepo.getValue(CommonOptions.DownloadPath)
            callback?.onResult(
                MoshiHelper.toJson(
                    RemoteDeviceStorage(
                        downloadPath,
                        File(downloadPath).freeSpace,
                        File(downloadPath).totalSpace
                    )
                )
            )
        }
    }

    override fun getTorrentList(callback: ITaskServiceCallback?) {

    }

    override fun uploadTorrentNotify(url: String?, callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun initMagnetUri(
        url: String?,
        torrentName: String?,
        callback: ITaskServiceCallback?
    ) {
        serviceScope.launch {
            handleInitMagnetUri(url, torrentName, callback)
        }
    }

    private suspend fun handleInitMagnetUri(
        url: String?,
        torrentName: String?,
        callback: ITaskServiceCallback?
    ) {
        if (url == null) {
            callback?.onFailed("url is null", TaskExecuteError.NOT_SUPPORT_URL.ordinal)
            return
        }
        val newTaskConfigModelResult = engineRepo.initUrl(url)
        if (newTaskConfigModelResult is IResult.Error) {
            callback?.onFailed(
                newTaskConfigModelResult.exception.message,
                newTaskConfigModelResult.code
            )
            return
        }
        val newTaskConfigModel = newTaskConfigModelResult as IResult.Success

        if (newTaskConfigModel.data is NewTaskConfigModel.TorrentTask) {
            val torrentId = newTaskConfigModel.data.torrentId
            val mapResult = torrentRepo.getTorrentById(torrentId)
            if (mapResult is IResult.Error) {
                callback?.onFailed(
                    mapResult.exception.message,
                    mapResult.code
                )
            }
            val torrentMap = (mapResult as IResult.Success).data
            torrentMap.keys.firstOrNull()?.let {
                callback?.onResult(MoshiHelper.toJson(it.asRemoteTorrentInfo(torrentMap[it])))
            }
        }
    }


    override fun dumpTorrentInfo(torrentPath: String?, callback: ITaskServiceCallback?) {
        serviceScope.launch {
            getTorrentInfo(torrentPath, callback)
        }
    }

    private suspend fun getTorrentInfo(torrentPath: String?, callback: ITaskServiceCallback?) {
        if (torrentPath == null) {
            callback?.onFailed("torrentPath is null", TaskExecuteError.NOT_SUPPORT_URL.ordinal)
            return
        }
        val result = engineRepo.getTorrentInfo(torrentPath)
        if (result is IResult.Error) {
            callback?.onFailed(result.exception.message, result.code)
            return
        }
        val dataMap = (result as IResult.Success).data
        dataMap.keys.firstOrNull()?.let {
            callback?.onResult(MoshiHelper.toJson(it.asRemoteTorrentInfo(dataMap[it])))
        }

    }

    override fun deleteTask(
        url: String?,
        isDeleteFile: Boolean,
        callback: ITaskServiceCallback?
    ) {
        serviceScope.launch {
            handleDeleteTask(url, isDeleteFile, callback)
        }
    }

    private fun handleDeleteTask(
        url: String?,
        deleteFile: Boolean,
        callback: ITaskServiceCallback?
    ) {
        if (url == null) {
            callback?.onFailed("url is null", TaskExecuteError.NOT_SUPPORT_URL.ordinal)
            return
        }
        service.deleteTask(url, deleteFile, callback)
    }

    override fun getTaskList(callback: ITaskServiceCallback?) {
        serviceScope.launch {
            handleGetTaskList(callback)
        }
    }

    private suspend fun handleGetTaskList(callback: ITaskServiceCallback?) {
        val taskList = taskRepo.getTasks()
        taskList.map {
            val taskId = service.getDownloadingTaskStatusMap()[it.url]?.taskId
            var torrentHash = ""
            if (it.torrentId > 0) {
                val hashResponse = torrentRepo.getTorrentHash(it.torrentId)
                if (hashResponse is IResult.Success) {
                    torrentHash = hashResponse.data
                }
            }
            RemoteTask(
                id = it.id,
                down_size = it.downloadSize,
                download_path = it.downloadPath,
                file_count = it.fileCount,
                is_multifile = it.fileCount > 1,
                is_torrent_task = it.torrentId > 0,
                status = when (it.status) {
                    DownloadTaskStatus.DOWNLOADING -> {
                        ITaskState.RUNNING.code
                    }

                    DownloadTaskStatus.COMPLETED -> {
                        ITaskState.DONE.code
                    }

                    else -> {
                        ITaskState.STOP.code
                    }
                },
                task_id = taskId?.id ?: INVALID_ID,
                task_name = it.name,
                total_size = it.totalSize,
                url = it.url,
                hash = torrentHash,
                create_time = it.createTime
            )
        }.let {
            callback?.onResult(MoshiHelper.toJson(it))
        }
    }

    override fun getDownloadStatus(url: String?, callback: ITaskServiceCallback?) {
        serviceScope.launch {
            handleTaskStatus(url, callback)
        }
    }

    private suspend fun handleTaskStatus(url: String?, callback: ITaskServiceCallback?) {
        if (url == null)
            return
        service.getDownloadingTaskStatusMap()[url]?.let {
            RemoteTaskStatus(
                download_size = it.downloadSize,
                speed = it.speed,
                status = it.status,
                url = it.url,
                is_done = it.status == ITaskState.DONE.code,
                total_size = it.totalSize,
                task_id = it.taskId.id
            ).let {
                callback?.onResult(MoshiHelper.toJson(it))
            }
        } ?: run {
            taskRepo.getTaskByUrl(url)?.let {
                RemoteTaskStatus(
                    download_size = it.downloadSize,
                    speed = 0,
                    status = when (it.status) {
                        DownloadTaskStatus.DOWNLOADING -> {
                            ITaskState.RUNNING.code
                        }

                        DownloadTaskStatus.COMPLETED -> {
                            ITaskState.DONE.code
                        }

                        else -> {
                            ITaskState.STOP.code
                        }
                    },
                    url = it.url,
                    is_done = it.status == DownloadTaskStatus.COMPLETED,
                    total_size = it.totalSize,
                    task_id = INVALID_ID
                ).let {
                    callback?.onResult(MoshiHelper.toJson(it))
                    return@run
                }
            }
            callback?.onFailed("task not found", TaskExecuteError.TASK_NOT_FOUND.ordinal)
        }
    }

    override fun setConfig(
        speedLimit: String?,
        maxThread: String?,
        downloadPath: String?,
        callback: ITaskServiceCallback?
    ) {
        serviceScope.launch {
            handleSetConfigSet(speedLimit, maxThread, downloadPath, callback)
        }
    }

    private suspend fun handleSetConfigSet(
        speedLimit: String?,
        maxThread: String?,
        downloadPath: String?,
        callback: ITaskServiceCallback?
    ) {
        speedLimit?.let {
            engineRepo.changeOption(XLOptions.SpeedLimit, it)
            engineRepo.changeOption(Aria2Options.SpeedLimit, it)
        }
        downloadPath?.let {
            engineRepo.changeOption(CommonOptions.DownloadPath, it)
        }
        maxThread?.let {
            engineRepo.changeOption(CommonOptions.MaxThread, it)
        }

        callback?.apply {
            RemoteSetDownloadConfig(
                configRepo.getValue(XLOptions.SpeedLimit).toLong(),
                configRepo.getValue(CommonOptions.MaxThread).toInt(),
                configRepo.getValue(CommonOptions.DownloadPath)
            ).let {
                onResult(MoshiHelper.toJson(it))
            }
        }
    }

    override fun getConfigSet(callback: ITaskServiceCallback?) {
        serviceScope.launch {
            handleGetConfigSet(callback)
        }
    }

    private suspend fun handleGetConfigSet(callback: ITaskServiceCallback?) {
        callback?.apply {
            val configSet = RemoteGetDownloadConfig(
                configRepo.getValue(XLOptions.SpeedLimit).toLong(),
                configRepo.getValue(CommonOptions.MaxThread).toInt(),
                configRepo.getValue(CommonOptions.DownloadPath)
            )
            onResult(MoshiHelper.toJson(configSet))
        }
    }

    override fun addServiceListener(listener: ITaskServiceListener?) {
        listener?.apply {
            serviceListenerList.add(TaskServiceListenerWrapper(listener))
        }
    }

    override fun removeServiceListener(listener: ITaskServiceListener?) {
        listener?.apply {
            for (listenWrapper in serviceListenerList) {
                if (listenWrapper.listener.tag == listener.tag) {
                    serviceListenerList.remove(listenWrapper)
                    break;
                }
            }
        }
    }

    fun sendToClient(command: String, data: String, expand: String?) {
        val iterator = serviceListenerList.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            try {
                it.listener.notify(command, data, expand)
            } catch (e: Exception) {
                iterator.remove()
            }
        }
    }

    fun sendErrorToClient(command: String, reason: String, code: Int) {
        val iterator = serviceListenerList.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            try {
                it.listener.failed(command, reason, code)
            } catch (e: Exception) {
                iterator.remove()
            }
        }
    }

    override fun DLogger.tag(): String {
        return "TaskStatusServiceImpl"
    }

    class TaskServiceListenerWrapper(val listener: ITaskServiceListener)

}