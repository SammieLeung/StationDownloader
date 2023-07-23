package com.station.stationdownloader.ui.fragment.downloading

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.R
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadingTaskViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository,
    val enginRepo: IEngineRepository
) : ViewModel(), DLogger {
    private val _taskList = MutableStateFlow<List<TaskItem>>(emptyList())
    val taskList = _taskList.asSharedFlow()
    private val _statusState: MutableStateFlow<StatusState> = MutableStateFlow(StatusState.Init)
    val statusState = _statusState.asStateFlow()

    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.getTaskList>()
        val updateProgress = actionStateFlow.filterIsInstance<UiAction.UpdateProgress>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartTask>()
        val stopTask = actionStateFlow.filterIsInstance<UiAction.StopTask>()

        handleGetTaskList(getTaskList)
        handleUpdateProgressFlow(updateProgress)
        handleStartTask(startTask)
        handleStopTask(stopTask)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.getTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            _taskList.update {
                taskRepo.getTasks().filter {
                    it.status!=DownloadTaskStatus.COMPLETED
                }.map {
                    it.asStationDownloadTask().asTaskItem()
                }
            }
        }
    }

    private fun handleStartTask(startTask: Flow<UiAction.StartTask>) = viewModelScope.launch {
        startTask.collect { action ->
            val xlDownloadTaskEntity = taskRepo.getTaskByUrl(action.url)
            xlDownloadTaskEntity?.let {
                _statusState.update {
                    val taskItem = _taskList.value.find { it.url == action.url }
                    taskItem?.let { item ->
                        StatusState.Status(
                            item.copy(
                                statuBtn = formatStatusBtn(ITaskState.RUNNING.ordinal)
                            )
                        )
                    } ?: StatusState.Init
                }
                val taskIdResult = enginRepo.startTask(it.asStationDownloadTask())
                if (taskIdResult is IResult.Error) {
                    _statusState.update {
                        val taskItem = _taskList.value.find { it.url == action.url }
                        taskItem?.let { item ->
                            StatusState.Status(
                                item.copy(
                                    statuBtn = formatStatusBtn(ITaskState.STOP.ordinal)
                                )
                            )
                        } ?: StatusState.Init
                    }
                    return@collect
                }



                taskIdResult as IResult.Success
                TaskService.watchTask(application, it.url, taskIdResult.data)
            }
        }
    }

    private fun handleStopTask(stopTask: Flow<UiAction.StopTask>) = viewModelScope.launch {
        stopTask.collect { action ->
            TaskService.cancelWatchTask(application, action.url)
            _statusState.update {
                val taskItem = _taskList.value.find { it.url == action.url }
                taskItem?.let { item ->
                    StatusState.Status(
                        item.copy(
                            statuBtn = formatStatusBtn(ITaskState.STOP.ordinal),
                            speed = ""
                        )
                    )
                } ?: StatusState.Init
            }
            val entity = taskRepo.getTaskByUrl(action.url)
            entity?.let {
                enginRepo.stopTask(action.taskId, entity.asStationDownloadTask())
            }
        }


    }

    private fun handleUpdateProgressFlow(updateProgress: Flow<UiAction.UpdateProgress>) =
        viewModelScope.launch {
            updateProgress.collect {

            }
        }

    fun setTaskStatus(taskStatus: StateFlow<Map<String, TaskStatus>>) {
        viewModelScope.launch {
            taskStatus.collectLatest {
                it.forEach { url, taskStatus ->
                    val taskItem = _taskList.value.find {
                        it.url == url
                    }

                    taskItem?.apply {
                        _statusState.update {
                            StatusState.Status(
                                taskItem.copy(
                                    taskId = taskStatus.taskId,
                                    speed = formatSpeed(taskStatus.speed),
                                    sizeInfo = formatSizeInfo(
                                        taskStatus.downloadSize,
                                        taskStatus.totalSize
                                    ),
                                    progress = formatProgress(
                                        taskStatus.downloadSize,
                                        taskStatus.totalSize
                                    ),
                                    statuBtn = formatStatusBtn(taskStatus.status)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun formatProgress(downloadSize: Long, totalSize: Long): Int {
        return TaskTools.formatProgress(downloadSize, totalSize)
    }

    private fun formatSizeInfo(downloadSize: Long, totalSize: Long): String {
        return TaskTools.formatSizeInfo(downloadSize, totalSize)
    }

    private fun formatSpeed(speed: Long): String {
        return TaskTools.formatSpeed(speed)
    }

    private fun formatStatusBtn(status: Int): Int {
        return when (status) {
            ITaskState.RUNNING.code -> {
                R.drawable.ic_stop
            }

            else -> {
                R.drawable.ic_start
            }
        }
    }

    override fun DLogger.tag(): String {
        return DownloadingTaskViewModel::class.java.simpleName
    }

    fun StationDownloadTask.asTaskItem(): TaskItem {
        return TaskItem(
            url = this.url,
            taskName = this.name,
            statuBtn = when (this.status) {
                DownloadTaskStatus.DOWNLOADING -> {
                    R.drawable.ic_stop
                }

                else -> {
                    R.drawable.ic_start
                }
            },
            progress = TaskTools.formatProgress(downloadSize, totalSize),
            sizeInfo = TaskTools.formatSizeInfo(downloadSize, totalSize),
            speed = "",
            downloadPath = this.downloadPath,
            engine = this.engine.name
        )
    }

}

sealed class StatusState {
    data class Status(val taskItem: TaskItem) : StatusState()
    object Init : StatusState()
}


sealed class UiAction {
    object getTaskList : UiAction()
    object UpdateProgress : UiAction()
    data class StartTask(val url: String) : UiAction()
    data class StopTask(val url: String, val taskId: Long) : UiAction()
}

data class TaskItem(
    val taskId: Long = -1L,
    val url: String,
    val taskName: String,
    val statuBtn: Int,
    val progress: Int = 50,
    val sizeInfo: String,
    val speed: String,
    val downloadPath: String,
    val engine: String
)




