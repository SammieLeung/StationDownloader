package com.station.stationdownloader.ui.fragment.downloading

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadTaskManageViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository
) : ViewModel() {
    private val _taskList =
        taskRepo.getTasksStream().map { if (it is IResult.Success) it.data else emptyList() }

    val downloadingTaskList =
        _taskList.map { it.filter { it.status != DownloadTaskStatus.COMPLETED } }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
    }


}
data class TaskItem(
    val taskName: String,
    val status: DownloadTaskStatus,
    val progress: Int,
    val sizeInfo: String,
    val speed: Long,
    val downloadPath: String,
    val engine: DownloadEngine
) {

}