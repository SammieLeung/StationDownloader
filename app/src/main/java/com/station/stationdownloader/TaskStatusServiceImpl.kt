package com.station.stationdownloader

import com.orhanobut.logger.Logger
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.remote.json.RemoteStartTask
import com.station.stationdownloader.data.source.remote.json.RemoteTask
import com.station.stationdownloader.data.source.remote.json.RemoteTaskStatus
import com.station.stationkitkt.MoshiHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskStatusServiceImpl(
    private val service: TaskService,
    private val serviceScope: CoroutineScope
) : ITaskStatusService.Stub() {
    private val entryPoint: TaskStatusServiceEntryPoint

    init {
        entryPoint = EntryPointAccessors.fromApplication(
            service.applicationContext,
            TaskStatusServiceEntryPoint::class.java
        )
    }

    private val taskRepo: IDownloadTaskRepository by lazy {
        entryPoint.getTaskRepo()
    }
    private val engineRepo: IEngineRepository by lazy {
        entryPoint.getEngineRepo()
    }
    private val configRepo: IConfigurationRepository by lazy {
        entryPoint.getConfigRepo()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TaskStatusServiceEntryPoint {
        fun getEngineRepo(): IEngineRepository
        fun getTaskRepo(): IDownloadTaskRepository
        fun getConfigRepo(): IConfigurationRepository
    }

    fun getService(): TaskService = service

    private suspend fun handleGetTaskList(callback: ITaskServiceCallback?) {
        val taskList = taskRepo.getTasks()
        taskList.map {
            val taskId = service.getRunningTaskMap()[it.url]?.taskId ?: -1L
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
                task_id = taskId,
                task_name = it.name,
                total_size = it.totalSize,
                url = it.url,
                create_time = it.createTime
            )
        }.let {
            callback?.onResult(MoshiHelper.toJson(it))
        }
    }

    private fun handleTaskStatus(url: String?, callback: ITaskServiceCallback?) {
        if (url == null)
            return
        service.getRunningTaskMap()[url]?.let {
            RemoteTaskStatus(
                download_size = it.downloadSize,
                speed = it.speed,
                status = it.status,
                url = it.url,
                is_done = it.status == ITaskState.DONE.code,
                total_size = it.totalSize,
                task_id = it.taskId
            ).let {
                callback?.onResult(MoshiHelper.toJson(it))
            }
        }
    }

    private suspend fun handleStartTask(
        url: String?,
        path: String?,
        selectIndexes: IntArray?,
        callback: ITaskServiceCallback?
    ) {
        if (url == null)
            return
        val newTaskResult = engineRepo.initUrl(url)
        if (newTaskResult is IResult.Error) {
            Logger.e(newTaskResult.exception.message.toString())
            callback?.onFailed(newTaskResult.exception.message, newTaskResult.code)
            return
        }
        newTaskResult as IResult.Success
        var newTaskConfig = newTaskResult.data

        if (selectIndexes == null || selectIndexes.isEmpty()) {
            taskRepo.getTaskByUrl(url)?.let {
                if (it.selectIndexes.isNotEmpty()) {
                    newTaskConfig.updateSelectIndexes(it.selectIndexes)
                }
            }
        } else {
            newTaskConfig.updateSelectIndexes(selectIndexes.toList())
        }

        path?.let {
            newTaskConfig = newTaskConfig.update(
                downloadPath = it
            )
        }

        val saveTaskResult = taskRepo.saveTask(
            newTaskConfig
        )

        if (saveTaskResult is IResult.Error) {
            when (saveTaskResult.code) {
                TaskExecuteError.REPEATING_TASK_NOTHING_CHANGED.ordinal -> {
                    saveTaskResult.exception.message?.let {
                        val status = service.getRunningTaskMap()[url]
                        if (status == null || status.taskId < 0 || status.status == ITaskState.STOP.code) {
                            startTaskAndWaitTaskId(url, callback)
                        } else {
                            callback?.onResult(
                                MoshiHelper.toJson(
                                    RemoteStartTask(
                                        url,
                                        service.getRunningTaskMap()[url]?.taskId ?: -1L
                                    )
                                )
                            )
                        }
                    }
                    Logger.e(service.getString(R.string.repeating_task_nothing_changed))
                }

                else -> {
                    Logger.e(saveTaskResult.exception.message.toString())
                    callback?.onFailed(saveTaskResult.exception.message, saveTaskResult.code)
                }
            }
            return
        }

        saveTaskResult as IResult.Success
        startTaskAndWaitTaskId(saveTaskResult.data.url, callback)
    }

    private suspend fun startTaskAndWaitTaskId(url: String, callback: ITaskServiceCallback?) {
        TaskService.startTask(
            service.applicationContext,
            url
        )
        while (service.getRunningTaskMap()[url] == null) {
            delay(10)
        }
        service.getRunningTaskMap()[url]?.let {
            callback?.onResult(MoshiHelper.toJson(RemoteStartTask(url, it.taskId)))
        }
    }

    override fun startTask(
        url: String?,
        path: String?,
        selectIndexes: IntArray?,
        callback: ITaskServiceCallback?
    ) {
        serviceScope.launch {
            handleStartTask(url, path, selectIndexes, callback)
        }
    }

    override fun stopTask(url: String?, callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun startAllTasks(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun stopAllTask(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun getDownloadPath(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun getTorrentList(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun uploadTorrentNotify(url: String?, callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun initMagnetUri(
        url: String?,
        torrentName: String?,
        callback: ITaskServiceCallback?
    ) {
        TODO("Not yet implemented")
    }

    override fun dumpTorrentInfo(torrentPath: String?, callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun deleteTask(url: String?, isDeleteFile: Boolean, callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

    override fun getTaskList(callback: ITaskServiceCallback?) {
        serviceScope.launch {
            handleGetTaskList(callback)
        }
    }

    override fun getDownloadStatus(url: String?, callback: ITaskServiceCallback?) {
        serviceScope.launch {
            handleTaskStatus(url, callback)
        }
    }

    override fun setConfig(
        speedLimit: Long,
        maxThread: Int,
        downloadPath: String?,
        callback: ITaskServiceCallback?
    ) {
        TODO("Not yet implemented")
    }

    override fun getConfigSet(callback: ITaskServiceCallback?) {
        TODO("Not yet implemented")
    }

}