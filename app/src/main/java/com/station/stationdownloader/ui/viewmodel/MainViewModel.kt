package com.station.stationdownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.FileType
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.filterFile
import com.station.stationdownloader.utils.DLogger
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
) : ViewModel(),DLogger {
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
        val actionResetAddUriState =
            actionStateFlow.filterIsInstance<DialogAction.ResetAddUriDialogState>()
        val actionResetTaskSettingState =
            actionStateFlow.filterIsInstance<DialogAction.ResetTaskSettingDialogState>()
        val actionCheckStateFlow = actionStateFlow.filterIsInstance<DialogAction.CheckState>()

        viewModelScope.launch {
            actionResetAddUriState.collect {
                _addUriState.value = AddUriUiState.INIT
            }
        }

        viewModelScope.launch {
            actionResetTaskSettingState.collect {
                _newTaskState.value = NewTaskState.INIT
            }
        }

        viewModelScope.launch {
            actionCheckStateFlow.collect {
                logger("actionCheckStateFlow collect")
                val filterType = it.fileType
                if(_newTaskState.value is NewTaskState.PreparingData){
                    _newTaskState.update { it ->
                        val oldData=it as NewTaskState.PreparingData
                        if (oldData.task._fileTree is TreeNode.Directory)
                            oldData.task._fileTree.filterFile(filterType, true)
                        oldData.copy()
                    }
                }

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
            if (it is NewTaskState.PreparingData) {
                it.copy(fileFilterGroup = it.fileFilterGroup.copy(selectVideo = true))
            } else {
                it
            }
        }
    }

    override fun DLogger.tag(): String {
        return MainViewModel::class.java.simpleName
    }
}

sealed class UiAction {
    data class InitTask(val url: String) : UiAction()
    data class StartDownloadTask(val task: NewTaskConfigModel) : UiAction()
}

sealed class DialogAction {
    object ResetAddUriDialogState : DialogAction()
    object ResetTaskSettingDialogState : DialogAction()

    data class CheckState(val fileType: FileType, val isSelect: Boolean) : DialogAction()

}

data class MainUiState(
    val isLoading: Boolean = false,
)

sealed class NewTaskState {
    object INIT : NewTaskState()
    data class PreparingData(
        val task: NewTaskConfigModel,
        val fileFilterGroup: fileFilterGroup = fileFilterGroup()
    ) : NewTaskState()

    object LOADING : AddUriUiState<Nothing>()
}

data class fileFilterGroup(
    val selectVideo: Boolean = true,
    val selectAudio: Boolean = false,
    val selectImage: Boolean = false,
    val selectOther: Boolean = false,
)


sealed class AddUriUiState<out T> {
    object INIT : AddUriUiState<Nothing>()
    object LOADING : AddUriUiState<Nothing>()
    object SUCCESS : AddUriUiState<Nothing>()
    data class ERROR(val errMsg: String = "") : AddUriUiState<Nothing>()
}
