package com.station.stationdownloader.ui.fragment.downloading

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.R
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadTaskManageViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository
) : ViewModel() {
    private val _taskList =
        taskRepo.getTasksStream().map {
            if (it is IResult.Success) it.data else emptyList()
        }


    val unCompletedTaskList =
        _taskList.map { it.filter { it.status != DownloadTaskStatus.COMPLETED } }

    val taskItemList = unCompletedTaskList.map {
        it.map {
            it.asStationDownloadTask().asTaskItem()
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val _statusState: MutableStateFlow<StatusState> = MutableStateFlow(StatusState.Init)
    val statusState = _statusState.asStateFlow()


    val downloadingTasks = unCompletedTaskList.map {
        it.filter {
            it.status == DownloadTaskStatus.DOWNLOADING
        }.map {
            it.asStationDownloadTask().asTaskItem()
        }
    }


    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val updateProgress = actionStateFlow.filterIsInstance<UiAction.UpdateProgress>()

        handleUpdateProgressFlow(updateProgress)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
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
                    val taskItem = taskItemList.value.filter {
                        it.url == url
                    }?.first()

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
                                    )
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

}

sealed class StatusState {
    data class Status(val taskItem: TaskItem) : StatusState()
    object Init : StatusState()
}


sealed class UiAction {
    object UpdateProgress : UiAction()
    object StopUpdateProgress : UiAction()
    data class StartTask(val url: String) : UiAction()
    data class StopTask(val taskId: Long) : UiAction()
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


fun StationDownloadTask.asTaskItem(): TaskItem {
    return TaskItem(
        url = this.url,
        taskName = this.name,
        statuBtn = when (this.status) {
            DownloadTaskStatus.PENDING -> {
                R.drawable.ic_start
            }

            DownloadTaskStatus.DOWNLOADING -> {
                R.drawable.ic_start
            }

            DownloadTaskStatus.COMPLETED -> {
                R.drawable.ic_start
            }

            DownloadTaskStatus.FAILED -> {
                R.drawable.ic_start
            }
        },
        progress = TaskTools.formatProgress(downloadSize, totalSize),
        sizeInfo = TaskTools.formatSizeInfo(downloadSize, totalSize),
        speed = "",
        downloadPath = "",
        engine = this.engine.name
    )
}

