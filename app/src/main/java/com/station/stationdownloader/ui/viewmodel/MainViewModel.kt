package com.station.stationdownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.utils.TaskTools
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

    private val _newTaskState = MutableStateFlow<NewTaskState>(NewTaskState.INIT)
    val newTaskState: StateFlow<NewTaskState> = _newTaskState.asStateFlow()

    val accept: (UiAction) -> Unit
    val dialogAccept: (DialogAction) -> Unit


    init {
        accept = initAcceptAction()
        dialogAccept = initAddUriDialogAcceptAction()
    }

    private fun initAcceptAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val initTask = actionStateFlow.filterIsInstance<UiAction.InitTask>()

        viewModelScope.launch {
            initTask.flatMapLatest {
                flow {
                    Logger.d("initTask flow")

                    _addUriState.update {
                        AddUriUiState.LOADING
                    }
                    _mainUiState.update {
                        it.copy(true)
                    }
                    val result = engineRepo.initUrl(it.url)
                    emit(result)
                }
            }.collect { result ->
                _mainUiState.update {
                    it.copy(isLoading = false)
                }
                updateAddUriState(result)
            }
        }



        return { action ->
            viewModelScope.launch {
                Logger.d("help")
                actionStateFlow.emit(action)
            }
        }
    }

    private fun initAddUriDialogAcceptAction(): (DialogAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<DialogAction> = MutableSharedFlow()
        val resetAddUriState =
            actionStateFlow.filterIsInstance<DialogAction.ResetAddUriDialogState>()
        val resetTaskSettingState =
            actionStateFlow.filterIsInstance<DialogAction.ResetTaskSettingDialogState>()

        viewModelScope.launch {
            resetAddUriState.collect {
                _addUriState.value = AddUriUiState.INIT
            }
        }

        viewModelScope.launch {
            resetTaskSettingState.collect {
                _newTaskState.value = NewTaskState.INIT
            }
        }

        return { dialogAction ->
            viewModelScope.launch {
                actionStateFlow.emit(dialogAction)
            }
        }
    }

    private fun updateAddUriState(result: IResult<NewTaskConfigModel>) {
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
                updateNewTaskConfig(result.data)
            }
        }
    }

    private fun updateNewTaskConfig(data: NewTaskConfigModel) {
        _newTaskState.update {
            NewTaskState.PreparingData(
               task = data
            )
        }
    }

    fun updateVideo() {
            _newTaskState.update {
                if(it is NewTaskState.PreparingData){
                    it.copy(selectVideo = true)
                }else{
                    it
                }
            }
    }


    companion object {
        const val VIDEO_FILE = 1
        const val AUDIO_FILE = 2
        const val PICTURE_FILE = 3
        const val OTHER_FILE = 4
    }
}

sealed class UiAction {
    data class InitTask(val url: String) : UiAction()
    data class StartDownloadTask(val task:NewTaskConfigModel):UiAction()
}

sealed class DialogAction {
    object ResetAddUriDialogState : DialogAction()
    object ResetTaskSettingDialogState : DialogAction()

    data class CheckAll(val fileType: Int) : DialogAction()
    data class UnCheckAll(val fileType: Int) : DialogAction()

}

data class MainUiState(
    val isLoading: Boolean = false,
)

sealed class NewTaskState {
    object INIT : NewTaskState()
    data class PreparingData(
        val task: NewTaskConfigModel,
        val selectVideo: Boolean = true,
        val selectAudio: Boolean = false,
        val selectImage: Boolean = false,
        val selectOther: Boolean = false,
    ) : NewTaskState()

    object LOADING : AddUriUiState<Nothing>()
}


sealed class AddUriUiState<out T> {
    object INIT : AddUriUiState<Nothing>()
    object LOADING : AddUriUiState<Nothing>()
    object SUCCESS : AddUriUiState<Nothing>()
    data class ERROR(val errMsg: String = "") : AddUriUiState<Nothing>()
}
