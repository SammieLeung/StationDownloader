package com.station.stationdownloader.ui.fragment.donetask

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.R
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskService.Companion.ACTION_DELETE_TASK_RESULT
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.ui.fragment.downloading.DownloadingTaskViewModel.Companion.ACTION_NOTIFY_ADD_DONE_TASK
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadedTaskViewModel @Inject constructor(
    val application: Application,
    val taskRepo: IDownloadTaskRepository,
) : ViewModel(),
    DLogger {

    private val _menuDialogUiState = MutableStateFlow(MenuDialogUiState())
    val menuDialogUiState = _menuDialogUiState.asStateFlow()

    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Init)
    val uiState = _uiState.asStateFlow()

    private val doneTaskItemList: MutableList<DoneTaskItem> = mutableListOf()

    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    ACTION_NOTIFY_ADD_DONE_TASK -> {
                        val url = it.getStringExtra("url") ?: return
                        handleAddDoneTask(url)
                    }

                    ACTION_DELETE_TASK_RESULT -> {
                        val url = it.getStringExtra("url") ?: ""
                        val result = it.getBooleanExtra("result", false)
                        val reason = it.getStringExtra("reason")
                        handleDeleteTaskCallback(url, result, reason)
                    }

                    else -> {}
                }
            }
        }
    }


    val intentFilter: IntentFilter by lazy {
        IntentFilter().apply {
            addAction(ACTION_NOTIFY_ADD_DONE_TASK)
            addAction(ACTION_DELETE_TASK_RESULT)
        }
    }

    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val initUiStateFlow = actionStateFlow.filterIsInstance<UiAction.InitUiState>()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.GetTaskList>()
        val showTaskMenuFlow = actionStateFlow.filterIsInstance<UiAction.ShowTaskMenu>()
        val openFileFlow = actionStateFlow.filterIsInstance<UiAction.OpenFile>()
        val isDeleteTaskFlow = actionStateFlow.filterIsInstance<UiAction.DeleteTask>()
        val showDeleteConfirmDialogFlow =
            actionStateFlow.filterIsInstance<UiAction.ShowDeleteConfirmDialog>()

        handleInitUiState(initUiStateFlow)
        handleGetTaskList(getTaskList)
        handleShowTaskMenu(showTaskMenuFlow)
        handleOpenFile(openFileFlow)
        handleDeleteTask(isDeleteTaskFlow)
        handleShowDeleteConfirmDialog(showDeleteConfirmDialogFlow)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleInitUiState(initUiStateFlow: Flow<UiAction.InitUiState>) =
        viewModelScope.launch {
            initUiStateFlow.collect {
                logger("UiState.Init")
                _uiState.update {
                    UiState.Init
                }
            }
        }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.GetTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            taskRepo.getTasks().filter {
                it.status == DownloadTaskStatus.COMPLETED
            }.map {
                it.asStationDownloadTask().asDoneTaskItem()
            }.let { taskItemList ->
                _uiState.update {
                    doneTaskItemList.clear()
                    doneTaskItemList.addAll(taskItemList)
                    if (it is UiState.FillDataList)
                        it.copy(doneTaskItemList = doneTaskItemList)
                    else
                        UiState.FillDataList(doneTaskItemList)
                }
            }
        }
    }

    private fun handleShowTaskMenu(showTaskMenu: Flow<UiAction.ShowTaskMenu>) =
        viewModelScope.launch {
            showTaskMenu.collect { action ->
                logger("showTaskMenu")
                _menuDialogUiState.update {
                    MenuDialogUiState(
                        url = action.url,
                        isShow = action.isShow,
                    )
                }
            }
        }


    private fun handleOpenFile(openFile: Flow<UiAction.OpenFile>) =
        viewModelScope.launch {
            openFile.collect { action ->
                val xlDownloadTaskEntity = taskRepo.getTaskByUrl(action.url) ?: return@collect
                if (xlDownloadTaskEntity.torrentId < 0) {
                    _uiState.update {
                        UiState.OpenFileState(
                            uri = Uri.fromFile(File(xlDownloadTaskEntity.downloadPath)),
                            isVideo = false
                        )
                    }
                } else {
                    val fileUri = Uri.fromFile(File(xlDownloadTaskEntity.downloadPath))
                    _uiState.update {
                        UiState.OpenFileState(
                            uri = fileUri,
                            isVideo = false
                        )
                    }
                }
            }
        }

    private fun handleDeleteTask(deleteTask: Flow<UiAction.DeleteTask>) =
        viewModelScope.launch {
            deleteTask.collect { action ->
                TaskService.deleteTask(application, action.url, action.isDeleteFile)
                _menuDialogUiState.update {
                    it.copy(isDeleting = true)
                }
            }
        }


    private fun handleDeleteTaskCallback(url: String, result: Boolean, reason: String?) {
        _uiState.update {
            val deleteItem =
                doneTaskItemList.find { it.url == url } ?: return
            doneTaskItemList.remove(deleteItem)
            UiState.DeleteTaskResultState(isSuccess = result, deleteItem = deleteItem, reason = reason ?: "")
        }
    }

    private fun handleShowDeleteConfirmDialog(showDeleteConfirmDialog: Flow<UiAction.ShowDeleteConfirmDialog>) =
        viewModelScope.launch {
            showDeleteConfirmDialog.collect { action ->
                _menuDialogUiState.update {
                    it.copy(isShowDelete = true)
                }
            }
        }

    private fun handleAddDoneTask(url: String) = viewModelScope.launch {
        val task = taskRepo.getTaskByUrl(url) ?: return@launch
        val taskItem = task.asStationDownloadTask().asDoneTaskItem()

        _uiState.update {
            doneTaskItemList.add(taskItem)
            UiState.AddDoneTaskItemState(isSuccess = true, doneItem = taskItem)
        }
    }

    override fun DLogger.tag(): String {
        return DownloadedTaskViewModel::class.java.simpleName
    }


    private fun StationDownloadTask.asDoneTaskItem(): DoneTaskItem {
        return DoneTaskItem(
            url = this.url,
            taskName = this.name,
            sizeInfo = TaskTools.formatSizeInfo(downloadSize, totalSize),
            downloadPath = this.downloadPath,
            engine = when(this.engine){
                DownloadEngine.XL->application.getString(R.string.xl_engine)
                DownloadEngine.ARIA2 -> application.getString(R.string.aria2_engine)
                else -> ""
            }
        )
    }
}

data class MenuDialogUiState(
    val url: String = "",
    val isDeleting: Boolean = false,
    val isShow: Boolean = false,
    val isShowDelete: Boolean = false
)

sealed class UiState {
    object Init : UiState()
    data class OpenFileState(val uri: Uri, val isVideo: Boolean) : UiState()
    data class FillDataList(val doneTaskItemList: List<DoneTaskItem>) : UiState()
    data class DeleteTaskResultState(
        val isSuccess: Boolean,
        val deleteItem: DoneTaskItem,
        val reason: String = ""
    ) : UiState()

    data class AddDoneTaskItemState(
        val isSuccess: Boolean,
        val doneItem: DoneTaskItem,
        val reason: String = ""
    ) : UiState()
}

sealed class UiAction {
    object InitUiState : UiAction()
    object GetTaskList : UiAction()
    data class ShowTaskMenu(val url: String = "", val isShow: Boolean) : UiAction()
    data class ShowDeleteConfirmDialog(val isShowDelete: Boolean) : UiAction()
    data class DeleteTask(val url: String, val isDeleteFile: Boolean) : UiAction()
    data class OpenFile(val url: String) : UiAction()
}

data class DoneTaskItem(
    val url: String,
    val taskName: String,
    val sizeInfo: String,
    val downloadPath: String,
    val engine: String
)

