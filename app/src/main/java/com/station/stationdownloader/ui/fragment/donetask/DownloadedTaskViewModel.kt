package com.station.stationdownloader.ui.fragment.donetask

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val _taskList = MutableStateFlow<List<DoneTaskItem>>(emptyList())
    val taskList = _taskList.asSharedFlow()

    private val _taskMenuState = MutableStateFlow<TaskMenuState>(TaskMenuState.Hide)
    val taskMenuState = _taskMenuState.asStateFlow()

    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.GetTaskList>()
        val showTaskMenuFlow = actionStateFlow.filterIsInstance<UiAction.ShowTaskMenu>()
        val hideTaskMenuFlow = actionStateFlow.filterIsInstance<UiAction.HideTaskMenu>()
        val getFileUriFlow = actionStateFlow.filterIsInstance<UiAction.GetFileUri>()

        handleGetTaskList(getTaskList)
        handleShowTaskMenu(showTaskMenuFlow)
        handleHideTaskMenu(hideTaskMenuFlow)
        handleGetFileUri(getFileUriFlow)
        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.GetTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            _taskList.update {
                taskRepo.getTasks().filter {
                    it.status == DownloadTaskStatus.COMPLETED
                }.map {
                    it.asStationDownloadTask().asDoneTaskItem()
                }
            }
        }
    }

    private fun handleShowTaskMenu(showTaskMenu: Flow<UiAction.ShowTaskMenu>) =
        viewModelScope.launch {
            showTaskMenu.collect { action ->
                _taskMenuState.update {
                    TaskMenuState.Show(action.url)
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

    private fun handleGetFileUri(getFileUri: Flow<UiAction.GetFileUri>) =
        viewModelScope.launch {
            getFileUri.collect { action ->
                val xlDownloadTaskEntity = taskRepo.getTaskByUrl(action.url) ?: return@collect
                val fileUri =
                    File(xlDownloadTaskEntity.downloadPath, xlDownloadTaskEntity.name).toUri()
                _taskMenuState.update {
                    TaskMenuState.FileUriState(fileUri)
                }
            }
        }

    override fun DLogger.tag(): String {
        return DownloadedTaskViewModel::class.java.simpleName
    }


    fun StationDownloadTask.asDoneTaskItem(): DoneTaskItem {
        return DoneTaskItem(
            url = this.url,
            taskName = this.name,
            sizeInfo = TaskTools.formatSizeInfo(downloadSize, totalSize),
            downloadPath = this.downloadPath,
            engine = this.engine.name
        )
    }
}

sealed class TaskMenuState {
    data class Show(val url: String) : TaskMenuState()
    object Hide : TaskMenuState()
    data class FileUriState(val fileUri: Uri) : TaskMenuState()
}

sealed class UiAction {
    object GetTaskList : UiAction()
    data class ShowTaskMenu(val url: String) : UiAction()
    object HideTaskMenu : UiAction()
    data class GetFileUri(val url: String) : UiAction()
}

data class DoneTaskItem(
    val url: String,
    val taskName: String,
    val sizeInfo: String,
    val downloadPath: String,
    val engine: String
)

