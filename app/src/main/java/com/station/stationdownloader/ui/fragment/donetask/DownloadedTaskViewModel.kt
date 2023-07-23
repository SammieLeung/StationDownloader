package com.station.stationdownloader.ui.fragment.donetask

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadedTaskViewModel @Inject constructor(
    val application: Application,
    val taskRepo: IDownloadTaskRepository,
) : ViewModel(),
    DLogger {
    private val _taskList = MutableStateFlow<List<DoneTaskItem>>(emptyList())
    val taskList = _taskList.asSharedFlow()

    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val getTaskList = actionStateFlow.filterIsInstance<UiAction.getTaskList>()

        handleGetTaskList(getTaskList)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleGetTaskList(getTaskList: Flow<UiAction.getTaskList>) = viewModelScope.launch {
        getTaskList.collect {
            _taskList.update {
                taskRepo.getTasks().map {
                    it.asStationDownloadTask().asDoneTaskItem()
                }
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

sealed class UiAction {
    object getTaskList : UiAction()
}

data class DoneTaskItem(
    val url: String,
    val taskName: String,
    val sizeInfo: String,
    val downloadPath: String,
    val engine: String
)

