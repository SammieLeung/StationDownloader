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
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.log

@HiltViewModel
class DownloadingTaskViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository,
    val enginRepo: IEngineRepository
) : ViewModel(), DLogger {
    private val _taskItemList = MutableStateFlow<List<TaskItem>>(emptyList())
    val taskItemList = _taskItemList.asStateFlow()

    private val _statusState: MutableStateFlow<StatusState> = MutableStateFlow(StatusState.Init)
    val statusState = _statusState.asStateFlow()

    private val _taskMenuState = MutableStateFlow<TaskMenuState>(TaskMenuState.Hide)
    val taskMenuState = _taskMenuState.asStateFlow()

    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.getTaskList>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartTask>()
        val stopTask = actionStateFlow.filterIsInstance<UiAction.StopTask>()
        val showTaskMenu = actionStateFlow.filterIsInstance<UiAction.ShowTaskMenu>()
        val hideTaskMenu = actionStateFlow.filterIsInstance<UiAction.HideTaskMenu>()
        val deleteTask = actionStateFlow.filterIsInstance<UiAction.DeleteTask>()

        handleGetTaskList(getTaskList)
        handleStartTask(startTask)
        handleStopTask(stopTask)
        handleTaskMenu(showTaskMenu)
        handleHideTaskMenu(hideTaskMenu)
        handleDeleteTask(deleteTask)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.getTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            _taskItemList.update {
                taskRepo.getTasks().filter {
                    it.status != DownloadTaskStatus.COMPLETED
                }.map {
                    it.asStationDownloadTask().asTaskItem()
                }
            }
        }
    }

    private fun handleStartTask(startTask: Flow<UiAction.StartTask>) = viewModelScope.launch {
        startTask.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem =
                    _taskItemList.value.find { it.url == action.url } ?: return@withContext
                _statusState.update {
                    StatusState.Status(
                        taskItem = taskItem.copy(
                            statuBtn = formatStatusBtn(ITaskState.RUNNING.code)
                        ),
                        taskStatus = ITaskState.UNKNOWN.code
                    )
                }
                TaskService.startTask(application, action.url)
            }
        }
    }


    private fun handleStopTask(stopTask: Flow<UiAction.StopTask>) = viewModelScope.launch {
        stopTask.collect { action ->
            withContext(Dispatchers.Default) {
                logger("$this handleStopTask")
                val taskItem =
                    _taskItemList.value.find { it.url == action.url } ?: return@withContext
                _statusState.update {
                    StatusState.Status(
                        taskItem = taskItem.copy(
                            statuBtn = formatStatusBtn(ITaskState.STOP.code)
                        ),
                        taskStatus = ITaskState.UNKNOWN.code
                    )
                }
                TaskService.stopTask(application, action.url)
            }
        }
    }

    private fun handleTaskMenu(showTaskMenu: Flow<UiAction.ShowTaskMenu>) = viewModelScope.launch {
        showTaskMenu.collect { action ->
            val taskItem = _taskItemList.value.find { it.url == action.url }
            _taskMenuState.update {
                TaskMenuState.Show(action.url, taskItem?.statuBtn == R.drawable.ic_stop)
            }
        }
    }

    private fun handleHideTaskMenu(hideTaskMenu: Flow<UiAction.HideTaskMenu>) =
        viewModelScope.launch {
            hideTaskMenu.collect {
                _taskMenuState.update {
                    TaskMenuState.Hide
                }
            }
        }

    private fun handleDeleteTask(deleteTask: Flow<UiAction.DeleteTask>) = viewModelScope.launch {
        deleteTask.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem =
                    _taskItemList.value.find { it.url == action.url } ?: return@withContext
                _statusState.update {
                    StatusState.Status(
                        taskItem = taskItem.copy(
                            statuBtn = formatStatusBtn(ITaskState.STOP.code)
                        ),
                        taskStatus = ITaskState.UNKNOWN.code
                    )
                }
                TaskService.stopTask(application, action.url)
                _taskItemList.update {
                    val mutableList = it.toMutableList()
                    mutableList.remove(taskItem)
                    mutableList.toList()
                }
                taskRepo.deleteTask(action.url)
            }
        }
    }


    fun setTaskStatus(taskStatus: StateFlow<Map<String, TaskStatus>>) {
        viewModelScope.launch {
            taskStatus.collectLatest {
                it.forEach { url, taskStatus ->
                    val taskItem = _taskItemList.value.find {
                        it.url == url
                    } ?: return@forEach

                    if (taskStatus.status == ITaskState.DONE.code) {
                        accept(UiAction.getTaskList)
                    }
                    _statusState.update {
                        StatusState.Status(
                            taskItem = taskItem.copy(
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
                            ),
                            taskStatus = taskStatus.status
                        )
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

sealed class TaskMenuState {
    data class Show(val url: String, val isTaskRunning: Boolean) : TaskMenuState()
    object Hide : TaskMenuState()
}

sealed class StatusState {
    data class Status(val taskItem: TaskItem, val taskStatus: Int) : StatusState()
    object Init : StatusState()
}


sealed class UiAction {
    object getTaskList : UiAction()
    data class ShowTaskMenu(val url: String) : UiAction()
    object HideTaskMenu : UiAction()
    data class DeleteTask(val url: String) : UiAction()
    data class StartTask(val url: String) : UiAction()
    data class StopTask(val url: String) : UiAction()
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




