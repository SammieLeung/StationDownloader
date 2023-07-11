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

    private val _taskSettingState = MutableStateFlow<TaskSettingState>(TaskSettingState.INIT)
    val taskSettingState: StateFlow<TaskSettingState> = _taskSettingState.asStateFlow()

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
                _taskSettingState.value = TaskSettingState.INIT
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
                if (result.data is NewTaskConfigModel.TorrentTask)
                    showTorrentFilesInfo(result.data)
            }
        }
    }

    private fun showTorrentFilesInfo(data: NewTaskConfigModel.TorrentTask) {
//        var fileStateList: List<FileTreeModel> = emptyList()
//        if (data.fileList.isNotEmpty()) {
//            fileStateList = data.fileList.mapIndexed { index, fileName ->
//                FileTreeModel.File(
//                    index,
//                    fileName,
//                    fileName.ext(),
//                    0L,
//                    index in data.selectIndexes
//                )
//
//            }
//        }
        _mainUiState.update {
            it.copy(isShowTorrentFilesInfo = true)
        }
//        _taskSettingState.update {
//            TaskSettingState.PreparingData(
//                name = data.taskName,
//                fileList = fileStateList,
//                engine = DownloadEngine.XL,
//                downloadPath = data.downloadPath
//            )
//        }
    }


    companion object {
        const val VIDEO_FILE = 1
        const val AUDIO_FILE = 2
        const val PICTURE_FILE = 3
        const val OTHER_FILE = 4
    }

    fun String.ext(): String = TaskTools.getExt(this)
}

sealed class UiAction {
    data class InitTask(val url: String) : UiAction()
}

sealed class DialogAction {
    object ResetAddUriDialogState : DialogAction()
    object ResetTaskSettingDialogState : DialogAction()

    data class CheckAll(val fileType: Int) : DialogAction()
    data class UnCheckAll(val fileType: Int) : DialogAction()

}

data class MainUiState(
    val isLoading: Boolean = false,
    val isShowTorrentFilesInfo: Boolean = false
)

sealed class TaskSettingState {
    object INIT : TaskSettingState()
    data class PreparingData(
        val name: String = "",
        val fileTree: TreeNode,
        val engine: DownloadEngine = DownloadEngine.XL,
        val downloadPath: String = "",
        val selectVideo: Boolean = true,
        val selectAudio: Boolean = false,
        val selectImage: Boolean = false,
        val selectOther: Boolean = false,
    ) : TaskSettingState()

    object LOADING : AddUriUiState<Nothing>()
}




sealed class AddUriUiState<out T> {
    object INIT : AddUriUiState<Nothing>()
    object LOADING : AddUriUiState<Nothing>()
    object SUCCESS : AddUriUiState<Nothing>()
    data class ERROR(val errMsg: String = "") : AddUriUiState<Nothing>()
}
