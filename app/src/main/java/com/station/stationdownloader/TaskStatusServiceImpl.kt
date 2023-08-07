package com.station.stationdownloader

import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.asStationDownloadTask
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.setSelectFileIndexes
import com.station.stationdownloader.data.source.remote.json.RemoteTask
import com.station.stationdownloader.data.source.remote.json.RemoteTaskStatus
import com.station.stationdownloader.ui.fragment.newtask.printFileTree
import com.station.stationkitkt.MoshiHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.internal.concurrent.Task

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
        //TODO 初始化url，检查selectIndexes是否为空，为空则使用默认的
        val newTaskResult = engineRepo.initUrl(url)
        if (newTaskResult is IResult.Error) {
            Logger.e(newTaskResult.exception.message.toString())
            callback?.onFailed(newTaskResult.exception.message)
            return
        }
        newTaskResult as IResult.Success
        var newTaskConfig = newTaskResult.data
        selectIndexes?.let {
            if (it.isNotEmpty()) {
                newTaskConfig.updateSelectIndexes(it.toList())
            }
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
            Logger.e(saveTaskResult.exception.message.toString())
            callback?.onFailed(saveTaskResult.exception.message)
            return
        }

        saveTaskResult as IResult.Success

        TaskService.startTask(
            service.applicationContext,
            saveTaskResult.data.url
        )


//        callback?.let {
//            it.onResult()
//        }

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