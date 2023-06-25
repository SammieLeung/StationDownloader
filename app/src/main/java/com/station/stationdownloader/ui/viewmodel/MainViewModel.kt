package com.station.stationdownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.IConfigurationRepository
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
) : ViewModel() {
    @Inject
    lateinit var engineRepo: IEngineRepository

    @Inject
    lateinit var configRepo: IConfigurationRepository

    @Inject
    lateinit var torrentInfoRepo: ITorrentInfoRepository

    private val _addUriState =
        MutableStateFlow<AddUriUiState<StationDownloadTask>>(AddUriUiState.INIT)
    val addUriUiState = _addUriState.asStateFlow()

    private val _mainUiState = MutableStateFlow(MainUiState(false))
    val mainUiState: StateFlow<MainUiState> = _mainUiState.asStateFlow()

    private val _configState = MutableStateFlow(TaskConfigState())
    val configState: StateFlow<TaskConfigState> = _configState.asStateFlow()

    val accept: (UiAction) -> Unit


    init {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val initTask = actionStateFlow.filterIsInstance<UiAction.InitTask>()

        viewModelScope.launch {
            initTask.flatMapLatest {
                flow {
                    _addUriState.update {
                        AddUriUiState.LOADING
                    }
                    _mainUiState.update {
                        it.copy(true)
                    }
                    val result = engineRepo.initTask(it.url)
                    emit(result)
                }
            }.collect { result ->
                _mainUiState.update {
                    it.copy(isLoading = false)
                }
                updateAddUriState(result)
            }
        }



        accept = { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }

    }

    private fun updateAddUriState(result: IResult<StationDownloadTask>) {
        when (result) {
            is IResult.Error -> {
                _addUriState.update {
                    AddUriUiState.ERROR(result.exception.message.toString())
                }
                Logger.e(result.exception.message.toString())
            }

            is IResult.Success -> {
                _addUriState.update {
                    AddUriUiState.SUCCESS
                }
                if (result.data.urlType == DownloadUrlType.TORRENT)
                    showTorrentFilesInfo(result.data)
            }
        }
    }

    private fun showTorrentFilesInfo(data: StationDownloadTask) {
        var fileStateList: List<FileState> = emptyList()
        if (data.fileList.isNotEmpty()) {
            fileStateList = data.fileList.mapIndexed { index, fileName ->
                FileState(fileName, index, index in data.selectIndexes)
            }
        }
        _mainUiState.update {
            it.copy(isShowTorrentFilesInfo = true)
        }
        _configState.update {
            TaskConfigState(name = data.name, fileList = fileStateList, engine = DownloadEngine.XL, downloadPath = data.downloadPath)
        }
    }
}

sealed class UiAction {
    data class InitTask(val url: String) : UiAction()
}

data class MainUiState(
    val isLoading: Boolean = false,
    val isShowTorrentFilesInfo: Boolean = false
)

data class TaskConfigState(
    val name: String = "",
    val fileList: List<FileState> = emptyList(),
    val engine: DownloadEngine = DownloadEngine.XL,
    val downloadPath: String = "",
    val selectVideo: Boolean = true,
    val selectAudio: Boolean = false,
    val selectImage: Boolean = false,
    val selectOther: Boolean = false,
)

data class FileState(
    val fileName: String,
    val fileIndex: Int,
    val isChecked: Boolean = false
)


sealed class AddUriUiState<out T> {
    object INIT : AddUriUiState<Nothing>()
    object LOADING : AddUriUiState<Nothing>()
    object SUCCESS : AddUriUiState<Nothing>()
    data class ERROR(val errMsg: String = "") : AddUriUiState<Nothing>()
}
