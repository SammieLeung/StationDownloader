package com.station.stationdownloader.ui.fragment.downloading

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.TaskId
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
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
import java.util.logging.Logger
import javax.inject.Inject

@HiltViewModel
class DownloadingTaskViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository,
    val enginRepo: DefaultEngineRepository
) : ViewModel(), DLogger {

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Init)
    val uiState = _uiState.asStateFlow()

    private val _menuState: MutableStateFlow<MenuDialogUiState> =
        MutableStateFlow(
            MenuDialogUiState(
                isTaskRunning = false,
                isDelete = false,
                isShow = false,
                isShowDelete = false
            )
        )
    val menuState = _menuState.asStateFlow()

    val accept: (UiAction) -> Unit
    val aria2Accept: (Aria2Action) -> Unit

    private val downloadingTaskItemList: MutableList<TaskItem> = mutableListOf()

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (intent.action) {
                    TaskService.ACTION_DELETE_TASK_RESULT -> {
                        val url = it.getStringExtra("url") ?: ""
                        val result = it.getBooleanExtra("result", false)
                        handleDeleteTaskResult(url, result)
                    }
                }
            }
        }
    }

    val intentFilter: IntentFilter by lazy {
        IntentFilter().apply {
            addAction(TaskService.ACTION_DELETE_TASK_RESULT)
        }
    }

    init {
        accept = initAction()
        aria2Accept = initAria2Action()
    }

    private fun initAria2Action(): (Aria2Action) -> Unit {
        val actionStateFlow: MutableSharedFlow<Aria2Action> = MutableSharedFlow()
        val startAria2TaskMonitorFlow =
            actionStateFlow.filterIsInstance<Aria2Action.StartAria2TaskMonitor>()
        val cancelAria2TaskMonitorFlow =
            actionStateFlow.filterIsInstance<Aria2Action.CancelAria2TaskMonitor>()

        handleStartAria2TaskMonitorAction(startAria2TaskMonitorFlow)
        handleCancelAria2TaskMonitorAction(cancelAria2TaskMonitorFlow)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleStartAria2TaskMonitorAction(startTaskMonitorFlow: Flow<Aria2Action.StartAria2TaskMonitor>) {
        viewModelScope.launch {
            startTaskMonitorFlow.collect {
                enginRepo.tellAll().forEach { aria2Task ->
//                    val taskItem =
//                        downloadingTaskItemList.find { it.url == aria2Task.taskStatus.url && it.engine == DownloadEngine.ARIA2.name }
//                            ?: return@forEach
//                    val index = downloadingTaskItemList.indexOf(taskItem)
//                    downloadingTaskItemList[index] = taskItem.copy(
//                        taskId = aria2Task.taskStatus.taskId,
//                        status = aria2Task.taskStatus.status,
//                        speed = formatSpeed(aria2Task.taskStatus.speed),
//                        sizeInfo = formatSizeInfo(
//                            aria2Task.taskStatus.downloadSize,
//                            aria2Task.taskStatus.totalSize
//                        ),
//                        progress = formatProgress(
//                            aria2Task.taskStatus.downloadSize,
//                            aria2Task.taskStatus.totalSize
//                        ),
//                    )
//                    _uiState.update {
//                        UiState.UpdateProgress(downloadingTaskItemList[index])
//                    }
                    logger("tellAll item:$aria2Task")

                    if (aria2Task.taskStatus.status == ITaskState.RUNNING.code) {
                        TaskService.watchTask(
                            application,
                            aria2Task.taskStatus.url,
                            aria2Task.taskStatus.taskId.id,
                            aria2Task.taskStatus.taskId.engine.name
                        )
                    }
                }
            }
        }
    }

    private fun handleCancelAria2TaskMonitorAction(cancelTaskMonitorFlow: Flow<Aria2Action.CancelAria2TaskMonitor>) {
        viewModelScope.launch {
            cancelTaskMonitorFlow.collect {
                TaskService.cancelAllWatchTask(application)
            }
        }
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val initUiState = actionStateFlow.filterIsInstance<UiAction.InitUiState>()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.GetTaskList>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartTask>()
        val stopTask = actionStateFlow.filterIsInstance<UiAction.StopTask>()
        val showTaskMenu = actionStateFlow.filterIsInstance<UiAction.ShowTaskMenu>()
        val deleteTask = actionStateFlow.filterIsInstance<UiAction.DeleteTask>()
        val initTaskList = actionStateFlow.filterIsInstance<UiAction.CheckTaskList>()
        val showDeleteConfirmDialog =
            actionStateFlow.filterIsInstance<UiAction.ShowDeleteConfirmDialog>()

        handleInitUiState(initUiState)
        handleGetTaskList(getTaskList)
        handleStartTask(startTask)
        handleStopTask(stopTask)
        handleTaskMenu(showTaskMenu)
        handleDeleteTask(deleteTask)
        handleInitTaskList(initTaskList)
        handleShowDeleteConfirmDialog(showDeleteConfirmDialog)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleInitUiState(initUiState: Flow<UiAction.InitUiState>) =
        viewModelScope.launch {
            initUiState.collect {
                _uiState.update {
                    UiState.Init
                }
            }
        }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.GetTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            taskRepo.getTasks().filter {
                it.status != DownloadTaskStatus.COMPLETED
            }.map {
                it.asStationDownloadTask().asTaskItem()
            }.let { taskItemList ->
                _uiState.update {
                    downloadingTaskItemList.clear()
                    downloadingTaskItemList.addAll(taskItemList)
                    if (it is UiState.FillTaskList)
                        it.copy(taskList = downloadingTaskItemList)
                    else
                        UiState.FillTaskList(downloadingTaskItemList)
                }
            }
        }
    }

    private fun handleStartTask(startTask: Flow<UiAction.StartTask>) = viewModelScope.launch {
        startTask.collect { action ->
            withContext(Dispatchers.Default) {
                Log.d(tag(), "handleStartTask")
                val taskItem =
                    downloadingTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = downloadingTaskItemList.indexOf(taskItem)
                downloadingTaskItemList[index] = taskItem.copy(
                    status = ITaskState.LOADING.code,
                )
                _uiState.update {
                    UiState.UpdateProgress(downloadingTaskItemList[index])
                }
                TaskService.startTask(application, action.url)
            }
        }
    }

    private fun handleStopTask(stopTask: Flow<UiAction.StopTask>) = viewModelScope.launch {
        stopTask.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem =
                    downloadingTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = downloadingTaskItemList.indexOf(taskItem)
                downloadingTaskItemList[index] = taskItem.copy(
                    status = ITaskState.STOP.code,
                    speed = ""
                )
                _uiState.update {
                    UiState.UpdateProgress(downloadingTaskItemList[index])
                }
                TaskService.stopTask(application, action.url)
            }
        }
    }

    private fun handleTaskMenu(showTaskMenu: Flow<UiAction.ShowTaskMenu>) = viewModelScope.launch {
        showTaskMenu.collect { action ->
            withContext(Dispatchers.Default) {
                val taskItem =
                    downloadingTaskItemList.find { it.url == action.url } ?: return@withContext
                val index = downloadingTaskItemList.indexOf(taskItem)
                if (action.isShow) {
                    _menuState.update {
                        it.copy(
                            url = action.url,
                            isTaskRunning = downloadingTaskItemList[index].status == ITaskState.RUNNING.code,
                            isDelete = false,
                            isShow = true,
                            isShowDelete = false
                        )
                    }
                } else {
                    _menuState.update {
                        it.copy(
                            isShow = false,
                        )
                    }
                }
            }
        }
    }


    private fun handleDeleteTask(deleteTask: Flow<UiAction.DeleteTask>) = viewModelScope.launch {
        deleteTask.collect { action ->
            TaskService.deleteTask(application, action.url, action.isDeleteFile)
        }
    }

    private fun handleInitTaskList(initTaskList: Flow<UiAction.CheckTaskList>) =
        viewModelScope.launch {
            initTaskList.collect {
                withContext(Dispatchers.Default) {
                    logger("handleInitTaskList")
                    val newList = downloadingTaskItemList.map {
                        it.copy(status = ITaskState.STOP.code)
                    }
                    downloadingTaskItemList.clear()
                    downloadingTaskItemList.addAll(newList)
                    newList.forEach {
                        logger("handleInitTaskList over ${it.status} ${it.taskName}")
                    }
                    _uiState.update {
                        UiState.FillTaskList(newList)
                    }
                }
            }
        }

    private fun handleShowDeleteConfirmDialog(cancelDeleteConfirmDialog: Flow<UiAction.ShowDeleteConfirmDialog>) =
        viewModelScope.launch {
            cancelDeleteConfirmDialog.collect { action ->
                withContext(Dispatchers.Default) {
                    _menuState.update {
                        it.copy(
                            isShowDelete = action.isShowDelete
                        )
                    }
                }
            }
        }

    private fun handleDeleteTaskResult(url: String, result: Boolean) {
        val taskItem = downloadingTaskItemList.find { it.url == url } ?: return
        val index = downloadingTaskItemList.indexOf(taskItem)

        _uiState.update {
            val deleteItem = downloadingTaskItemList[index]
            downloadingTaskItemList.remove(deleteItem)
            UiState.DeleteTaskResultState(result, deleteItem)
        }

        _menuState.update {
            MenuDialogUiState(
                url = url,
                isTaskRunning = false,
                isDelete = true,
                isShow = false,
                isShowDelete = false
            )
        }
    }


    fun collectTaskStatus(taskStatus: StateFlow<TaskStatus>) {
        viewModelScope.launch {
            taskStatus.collect { taskStatus ->
                val url = taskStatus.url
                val taskItem = downloadingTaskItemList.find {
                        it.url == url
                } ?: return@collect
                val index = downloadingTaskItemList.indexOf(taskItem)
                logger("collectTaskStatus $url ${index}")
                if (taskStatus.status == ITaskState.RUNNING.code) {
                    downloadingTaskItemList[index] = taskItem.copy(
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
                    downloadingTaskItemList[index] = taskItem.copy(
                        taskId = taskStatus.taskId,
                        speed = "",
                        status = taskStatus.status
                    )
                }

                _uiState.update {
                    UiState.UpdateProgress(downloadingTaskItemList[index])
                }

                if (taskStatus.status == ITaskState.DONE.code) {
                    accept(UiAction.GetTaskList)
                    LocalBroadcastManager.getInstance(application).sendBroadcast(
                        Intent(ACTION_NOTIFY_ADD_DONE_TASK).apply {
                            putExtra(EXTRA_URL, url)
                        }
                    )
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

    override fun DLogger.tag(): String {
        return DownloadingTaskViewModel::class.java.simpleName
    }

    fun StationDownloadTask.asTaskItem(): TaskItem {
        return TaskItem(
            url = this.url,
            realUrl = this.realUrl,
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


    companion object {
        const val ACTION_NOTIFY_ADD_DONE_TASK = "action.notify.add.done.task"
        const val EXTRA_URL = "extra_url"
    }

}

data class MenuDialogUiState(
    val url: String = "",
    val isTaskRunning: Boolean,
    val isDelete: Boolean,
    val isShow: Boolean,
    val isShowDelete: Boolean
)

sealed class UiState {
    object Init : UiState()
    data class FillTaskList(val taskList: List<TaskItem>) : UiState()
    data class UpdateProgress(val taskItem: TaskItem) : UiState()

    data class DeleteTaskResultState(
        val isSuccess: Boolean,
        val deleteItem: TaskItem,
        val reason: String = ""
    ) : UiState()
}


data class TaskItem(
    val taskId: TaskId = TaskId.INVALID_TASK_ID,
    val url: String,
    val realUrl: String,
    val taskName: String,
    val status: Int,
    val progress: Int = 50,
    val sizeInfo: String,
    val speed: String,
    val downloadPath: String,
    val engine: String
)

sealed class UiAction {
    object InitUiState : UiAction()
    object GetTaskList : UiAction()

    data class StartTask(val url: String) : UiAction()
    data class StopTask(val url: String) : UiAction()
    object CheckTaskList : UiAction()
    data class ShowTaskMenu(val url: String = "", val isShow: Boolean) : UiAction()
    data class DeleteTask(val url: String, val isDeleteFile: Boolean) : UiAction()
    data class ShowDeleteConfirmDialog(val isShowDelete: Boolean) : UiAction()
}

sealed class Aria2Action {
    object StartAria2TaskMonitor : Aria2Action()
    object CancelAria2TaskMonitor : Aria2Action()
}

