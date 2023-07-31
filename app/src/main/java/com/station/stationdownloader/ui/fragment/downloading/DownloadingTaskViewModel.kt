package com.station.stationdownloader.ui.fragment.downloading

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.R
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DownloadingTaskViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository,
    val enginRepo: IEngineRepository
) : ViewModel(), DLogger {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Init)
    val uiState = _uiState.asStateFlow()

    val accept: (UiAction) -> Unit

    private val tmpTaskItemList: MutableList<TaskItem> = mutableListOf()

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.GetTaskList>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartTask>()
        val stopTask = actionStateFlow.filterIsInstance<UiAction.StopTask>()
        val showTaskMenu = actionStateFlow.filterIsInstance<UiAction.ShowTaskMenu>()
        val hideTaskMenu = actionStateFlow.filterIsInstance<UiAction.HideTaskMenu>()
        val deleteTask = actionStateFlow.filterIsInstance<UiAction.DeleteTask>()
        val initTaskList = actionStateFlow.filterIsInstance<UiAction.CheckTaskList>()

        handleGetTaskList(getTaskList)
        handleStartTask(startTask)
        handleStopTask(stopTask)
        handleTaskMenu(showTaskMenu)
        handleHideTaskMenu(hideTaskMenu)
        handleDeleteTask(deleteTask)
        handleInitTaskList(initTaskList)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.GetTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            logger("getTaskList")
            taskRepo.getTasks().filter {
                it.status != DownloadTaskStatus.COMPLETED
            }.map {
                it.asStationDownloadTask().asTaskItem()
            }.let { taskItemList ->
                logger("taskItemList size ${taskItemList.size}")
                _uiState.update {
                    tmpTaskItemList.clear()
                    tmpTaskItemList.addAll(taskItemList)
                    if (it is UiState.FillTaskList)
                        it.copy(taskList = tmpTaskItemList)
                    else
                        UiState.FillTaskList(tmpTaskItemList)
                }
                _uiState.update {
                    UiState.Init
                }
            }
        }
    }

    private fun handleStartTask(startTask: Flow<UiAction.StartTask>) = viewModelScope.launch {
        startTask.collect { action ->
            withContext(Dispatchers.Default) {
                Log.d(tag(), "handleStartTask")
                val taskItem = tmpTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = tmpTaskItemList.indexOf(taskItem)
                tmpTaskItemList[index] = taskItem.copy(
                    status = ITaskState.LOADING.code,
                    speed = formatSpeed(0L)
                )
                _uiState.update {
                    UiState.UpdateProgress(tmpTaskItemList[index])
                }
                TaskService.startTask(application, action.url)
            }
        }
    }

    private fun handleStopTask(stopTask: Flow<UiAction.StopTask>) = viewModelScope.launch {
        stopTask.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem = tmpTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = tmpTaskItemList.indexOf(taskItem)
                tmpTaskItemList[index] = taskItem.copy(
                    status = ITaskState.STOP.code,
                    speed = ""
                )
                _uiState.update {
                    UiState.UpdateProgress(tmpTaskItemList[index])
                }
                TaskService.stopTask(application, action.url)
            }
        }
    }

    private fun handleTaskMenu(showTaskMenu: Flow<UiAction.ShowTaskMenu>) = viewModelScope.launch {
        showTaskMenu.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem = tmpTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = tmpTaskItemList.indexOf(taskItem)
                _uiState.update {
                    UiState.ShowMenu(
                        action.url,
                        tmpTaskItemList[index].status == ITaskState.RUNNING.code
                    )
                }
            }
        }
    }

    private fun handleHideTaskMenu(hideTaskMenu: Flow<UiAction.HideTaskMenu>) =
        viewModelScope.launch {
            hideTaskMenu.collect {
                withContext(Dispatchers.Default) {
                    _uiState.update {
                        UiState.HideMenu
                    }
                }
            }
        }

    private fun handleDeleteTask(deleteTask: Flow<UiAction.DeleteTask>) = viewModelScope.launch {
        deleteTask.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem = tmpTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = tmpTaskItemList.indexOf(taskItem)
                TaskService.stopTask(application, action.url)
                tmpTaskItemList[index] = tmpTaskItemList[index].copy(status = ITaskState.STOP.code)
                _uiState.update {
                    UiState.UpdateProgress(tmpTaskItemList[index])
                }
                taskRepo.deleteTask(action.url)
                _uiState.update {
                    tmpTaskItemList.remove(tmpTaskItemList[index])
                    UiState.FillTaskList(tmpTaskItemList)
                }
            }
        }
    }


    private fun handleInitTaskList(initTaskList: Flow<UiAction.CheckTaskList>) =
        viewModelScope.launch {
            initTaskList.collect {
                withContext(Dispatchers.Default) {
                    logger("handleInitTaskList")
                    val newList = tmpTaskItemList.map {
                        it.copy(status = ITaskState.STOP.code)
                    }
                    tmpTaskItemList.clear()
                    tmpTaskItemList.addAll(newList)
                    newList.forEach {
                        logger("handleInitTaskList over ${it.status} ${it.taskName}")
                    }
                    _uiState.update {
                        UiState.FillTaskList(newList)
                    }
                }
            }
        }


    fun setTaskStatus(taskStatus: StateFlow<TaskStatus>) {
        viewModelScope.launch {
            taskStatus.collect { taskStatus ->
                val url = taskStatus.url
                val taskItem = tmpTaskItemList.find {
                    it.url == url
                } ?: return@collect
                val index = tmpTaskItemList.indexOf(taskItem)
                if (taskStatus.status == ITaskState.RUNNING.code) {
                    tmpTaskItemList[index] = taskItem.copy(
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
                        status = taskStatus.status
                    )
                } else {
                    tmpTaskItemList[index] = taskItem.copy(
                        taskId = taskStatus.taskId,
                        speed = "",
                        status = taskStatus.status
                    )
                }

                _uiState.update {
                    UiState.UpdateProgress(tmpTaskItemList[index])
                }

                if (taskStatus.status == ITaskState.DONE.code) {
                    accept(UiAction.GetTaskList)
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
            status = when (this.status) {
                DownloadTaskStatus.DOWNLOADING -> {
                    ITaskState.RUNNING.code
                }

                DownloadTaskStatus.PENDING, DownloadTaskStatus.PAUSE, DownloadTaskStatus.FAILED -> {
                    ITaskState.STOP.code
                }

                DownloadTaskStatus.COMPLETED -> {
                    ITaskState.DONE.code
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


sealed class UiState {
    object Init : UiState()
    data class FillTaskList(val taskList: List<TaskItem>) : UiState()
    data class UpdateProgress(val taskItem: TaskItem) : UiState()
    data class ShowMenu(val url: String, val isTaskRunning: Boolean) : UiState()
    object HideMenu : UiState()
}

data class TaskItem(
    val taskId: Long = -1L,
    val url: String,
    val taskName: String,
    val status: Int,
    val progress: Int = 50,
    val sizeInfo: String,
    val speed: String,
    val downloadPath: String,
    val engine: String
)

sealed class UiAction {
    object GetTaskList : UiAction()
    data class ShowTaskMenu(val url: String) : UiAction()
    object HideTaskMenu : UiAction()
    data class StartTask(val url: String) : UiAction()
    data class StopTask(val url: String) : UiAction()
    object CheckTaskList : UiAction()
    data class DeleteTask(val url: String) : UiAction()
}

