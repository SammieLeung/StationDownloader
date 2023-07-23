package com.station.stationdownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.FileType
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.filterFile
import com.station.stationdownloader.data.source.local.room.entities.asStationDownloadTask
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val taskRepo: IDownloadTaskRepository
) : ViewModel(), DLogger {
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

    private val _taskIdMap = MutableStateFlow(mapOf<String, Long>())
    val taskIdMap = _taskIdMap.asStateFlow()

    val accept: (UiAction) -> Unit
    val dialogAccept: (DialogAction) -> Unit


    init {
        accept = initAcceptAction()
        dialogAccept = initAddUriDialogAcceptAction()
    }

    private fun initAcceptAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val initTask = actionStateFlow.filterIsInstance<UiAction.InitTask>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartDownloadTask>()
        val resetToast = actionStateFlow.filterIsInstance<UiAction.ResetToast>()

        handleInitTaskAction(initTask)
        handleStartTaskAction(startTask)
        handleResetToast(resetToast)


        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleStartTaskAction(startTaskFlow: Flow<UiAction.StartDownloadTask>) =
        viewModelScope.launch {
            startTaskFlow.collect { it ->
                if (_newTaskState.value !is NewTaskState.PreparingData)
                    return@collect

                val saveTaskResult =
                    taskRepo.saveTask((_newTaskState.value as NewTaskState.PreparingData).task)
                if (saveTaskResult is IResult.Error) {
                    _mainUiState.update {
                        it.copy(toastState = ToastState.Toast(saveTaskResult.exception.message.toString()))
                    }
                    return@collect
                }


                saveTaskResult as IResult.Success

                val taskIdResult =
                    engineRepo.startTask(saveTaskResult.data.asStationDownloadTask())
                if (taskIdResult is IResult.Error) {
                    _mainUiState.update {
                        it.copy(toastState = ToastState.Toast(taskIdResult.exception.message.toString()))
                    }
                    return@collect
                }

                TaskService.watchTask(
                    application,
                    saveTaskResult.data.url,
                    (taskIdResult as IResult.Success).data
                )


                val taskId = taskIdResult.data

                val currentMap = _taskIdMap.value.toMutableMap()
                currentMap[saveTaskResult.data.url] = taskId
                _taskIdMap.update {
                    currentMap.toMap()
                }

                _newTaskState.update {
                    NewTaskState.SUCCESS
                }
            }
        }

    private fun handleInitTaskAction(initTaskFlow: Flow<UiAction.InitTask>) =
        viewModelScope.launch {
            initTaskFlow.flatMapLatest {
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
                if (result is IResult.Error) {
                    _addUriState.update {
                        AddUriUiState.ERROR(result.exception.message.toString())
                    }
                    Logger.e(result.exception.message.toString())
                    return@collect
                }

                _addUriState.update {
                    AddUriUiState.SUCCESS
                }

                val newTaskModel = (result as IResult.Success).data
                _newTaskState.update {
                    NewTaskState.PreparingData(
                        task = newTaskModel
                    )
                }
            }
        }

    private fun handleResetToast(resetToastFlow: Flow<UiAction.ResetToast>) =
        viewModelScope.launch {
            resetToastFlow.collect {
                _mainUiState.update {
                    it.copy(toastState = ToastState.INIT)
                }
            }
        }


    private fun initAddUriDialogAcceptAction(): (DialogAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<DialogAction> = MutableSharedFlow()
        val initAddUriState =
            actionStateFlow.filterIsInstance<DialogAction.ResetAddUriDialogState>()
        val initTaskSettingState =
            actionStateFlow.filterIsInstance<DialogAction.ResetTaskSettingDialogState>()
        val filterStateFlow = actionStateFlow.filterIsInstance<DialogAction.FilterGroupState>()

        val setDownloadPathFlow = actionStateFlow.filterIsInstance<DialogAction.SetDownloadPath>()

        handleInitAddUriState(initAddUriState)
        handleInitTaskSettingState(initTaskSettingState)
        handleFilterState(filterStateFlow)
        handleSetDownloadPathFlow(setDownloadPathFlow)


        return { dialogAction ->
            viewModelScope.launch {
                actionStateFlow.emit(dialogAction)
            }
        }
    }


    private fun handleInitAddUriState(initAddUriStateFlow: Flow<DialogAction.ResetAddUriDialogState>) =
        viewModelScope.launch {
            initAddUriStateFlow.collect {
                _addUriState.value = AddUriUiState.INIT
            }
        }

    private fun handleInitTaskSettingState(initTaskSettingStateFlow: Flow<DialogAction.ResetTaskSettingDialogState>) =
        viewModelScope.launch {
            initTaskSettingStateFlow.collect {
                _newTaskState.value = NewTaskState.INIT
            }
        }

    private fun handleFilterState(filterStateFlow: Flow<DialogAction.FilterGroupState>) =
        viewModelScope.launch {
            filterStateFlow.collect { checkState ->
                logger("actionCheckStateFlow collect")
                checkState.fileType
                _newTaskState.update {
                    if (it is NewTaskState.PreparingData) {
                        if (it.task._fileTree is TreeNode.Directory) {
                            it.task._fileTree.filterFile(checkState.fileType, checkState.isSelect)
                        }
                        val filterGroup = when (checkState.fileType) {
                            FileType.VIDEO -> it.fileFilterGroup.copy(selectVideo = checkState.isSelect)
                            FileType.AUDIO -> it.fileFilterGroup.copy(selectAudio = checkState.isSelect)
                            FileType.IMG -> it.fileFilterGroup.copy(selectImage = checkState.isSelect)
                            FileType.OTHER -> it.fileFilterGroup.copy(selectOther = checkState.isSelect)
                        }
                        it.copy(it.task, filterGroup)
                    } else {
                        it
                    }
                }
            }
        }

    private fun handleSetDownloadPathFlow(setDownloadPathFlow: Flow<DialogAction.SetDownloadPath>) =
        viewModelScope.launch {
            setDownloadPathFlow.collect { setDownloadPath ->
                if (_newTaskState.value is NewTaskState.PreparingData) {
                    _newTaskState.update {
                        (it as NewTaskState.PreparingData).copy(
                            task = it.task.update(
                                downloadPath = setDownloadPath.downloadPath
                            )
                        )
                    }
                }
            }
        }

    override fun DLogger.tag(): String {
        return MainViewModel::class.java.simpleName
    }
}

sealed class UiAction {
    data class InitTask(val url: String) : UiAction()
    object ResetToast : UiAction()
    object StartDownloadTask : UiAction()
}

sealed class DialogAction {
    object ResetAddUriDialogState : DialogAction()
    object ResetTaskSettingDialogState : DialogAction()

    data class FilterGroupState(val fileType: FileType, val isSelect: Boolean) : DialogAction()
    data class SetDownloadPath(val downloadPath: String) : DialogAction()
}

data class MainUiState(
    val isLoading: Boolean = false,
    val toastState: ToastState = ToastState.INIT,
)


sealed class ToastState {
    object INIT : ToastState()
    data class Toast(val msg: String) : ToastState()
}

sealed class NewTaskState {
    object INIT : NewTaskState()
    data class PreparingData(
        val task: NewTaskConfigModel,
        val fileFilterGroup: fileFilterGroup = fileFilterGroup()
    ) : NewTaskState()

    object SUCCESS : NewTaskState()

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
