package com.station.stationdownloader.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.FreeSpaceState
import com.station.stationdownloader.FileType
import com.station.stationdownloader.R
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskStatus
import com.station.stationdownloader.contants.TaskExecuteError
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.filterFile
import com.station.stationdownloader.data.source.repository.DefaultConfigurationRepository
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.ui.fragment.newtask.toHumanReading
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.MB
import com.station.stationdownloader.utils.XLEngineTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
    val taskRepo: IDownloadTaskRepository
) : ViewModel(), DLogger {
    @Inject
    lateinit var engineRepo: DefaultEngineRepository

    @Inject
    lateinit var configRepo: DefaultConfigurationRepository

    @Inject
    lateinit var torrentInfoRepo: ITorrentInfoRepository

    private val _addUriState =
        MutableStateFlow<AddUriUiState<StationDownloadTask>>(AddUriUiState.INIT)
    val addUriUiState = _addUriState.asStateFlow()

    private val _mainUiState = MutableStateFlow(MainUiState(false))
    val mainUiState: StateFlow<MainUiState> = _mainUiState.asStateFlow()

    private val _toastState = MutableStateFlow<ToastState>(ToastState.INIT)
    val toastState = _toastState.asStateFlow()

    private val _newTaskState = MutableStateFlow<NewTaskState>(NewTaskState.INIT)
    val newTaskState: StateFlow<NewTaskState> = _newTaskState.asStateFlow()


    val accept: (UiAction) -> Unit
    val dialogAccept: (DialogAction) -> Unit
    val emitToast: (ToastAction) -> Unit
    lateinit var downloadingTaskStatusInService: WeakReference<Map<String, TaskStatus>>

    init {
        accept = initAcceptAction()
        dialogAccept = initAddUriDialogAcceptAction()
        emitToast = initEmitToastAction()
    }

    override fun DLogger.tag(): String {
        return MainViewModel::class.java.simpleName
    }

    fun assertTorrentFile(path: String): Boolean {
        return XLEngineTools.assertTorrentFile(path)
    }

    private fun initAcceptAction(): (UiAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val initTask = actionStateFlow.filterIsInstance<UiAction.InitTask>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartDownloadTask>()
        val saveSession = actionStateFlow.filterIsInstance<UiAction.SaveSession>()

        handleInitTaskAction(initTask)
        handleStartTaskAction(startTask)
        handleSaveSessionAction(saveSession)


        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleSaveSessionAction(saveSession: Flow<UiAction.SaveSession>) =
        viewModelScope.launch {
            saveSession.collect {
                engineRepo.saveSession()
            }
        }

    private fun handleStartTaskAction(startTaskFlow: Flow<UiAction.StartDownloadTask>) =
        viewModelScope.launch {
            startTaskFlow.collect { action ->
                logger("startTaskFlow collect")
                if (_newTaskState.value !is NewTaskState.PreparingData)
                    return@collect
                val preparingData = _newTaskState.value as NewTaskState.PreparingData

                val saveTaskResult =
                    taskRepo.validateAndPersistTask(preparingData.task.update(downloadEngine = action.engine))
                if (saveTaskResult is IResult.Error) {
                    when (saveTaskResult.code) {
                        TaskExecuteError.REPEATING_TASK_NOTHING_CHANGED.ordinal -> {
                            _toastState.update {
                                ToastState.Show(application.getString(R.string.repeating_task_nothing_changed))
                            }
                        }

                        else -> _toastState.update {
                            Logger.e(saveTaskResult.exception.message.toString())
                            ToastState.Show(saveTaskResult.exception.message.toString())
                        }
                    }
                    return@collect
                }

                saveTaskResult as IResult.Success

                TaskService.startTask(application, saveTaskResult.data.url)

                _toastState.update {
                    ToastState.Show(application.getString(R.string.start_to_download))
                }
                _newTaskState.update {
                    NewTaskState.Success
                }
            }
        }

    private fun handleInitTaskAction(initTaskFlow: Flow<UiAction.InitTask>) =
        viewModelScope.launch {
            initTaskFlow.flatMapLatest {
                flow {
                    _addUriState.update {
                        AddUriUiState.LOADING
                    }
                    _mainUiState.update {
                        it.copy(isLoading = true)
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
                        task = newTaskModel,
                    )
                }
                _mainUiState.update {
                    it.copy(isShowAddNewTask = true)
                }
                dialogAccept(DialogAction.CalculateSizeInfo)
            }
        }


    private fun initAddUriDialogAcceptAction(): (DialogAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<DialogAction> = MutableSharedFlow()
        val initAddUriState =
            actionStateFlow.filterIsInstance<DialogAction.ResetAddUriDialogState>()
        val initTaskSettingState =
            actionStateFlow.filterIsInstance<DialogAction.ResetTaskSettingDialogState>()
        val filterStateFlow = actionStateFlow.filterIsInstance<DialogAction.FilterGroupState>()

        val changeDownloadPathFlow =
            actionStateFlow.filterIsInstance<DialogAction.ChangeDownloadPath>()
        val changeDownloadEngineFlow =
            actionStateFlow.filterIsInstance<DialogAction.ChangeDownloadEngine>()
        val calculateSizeInfoFlow =
            actionStateFlow.filterIsInstance<DialogAction.CalculateSizeInfo>()


        handleInitAddUriState(initAddUriState)
        handleInitTaskSettingState(initTaskSettingState)
        handleFilterState(filterStateFlow)
        handleChangeDownloadPathFlow(changeDownloadPathFlow)
        handleCalculateSizeInfo(calculateSizeInfoFlow)
        handleDownloadEngineFlow(changeDownloadEngineFlow)

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
                _mainUiState.update {
                    it.copy(isShowAddNewTask = false)
                }
            }
        }

    private fun handleFilterState(filterStateFlow: Flow<DialogAction.FilterGroupState>) =
        viewModelScope.launch {
            filterStateFlow.collect { checkState ->
                logger("actionCheckStateFlow collect")
                _newTaskState.update {
                    if (it is NewTaskState.PreparingData) {
                        it.task._fileTree as TreeNode.Directory
                        it.task._fileTree.filterFile(checkState.fileType, checkState.isSelect)
                        val filterGroup = when (checkState.fileType) {
                            FileType.VIDEO -> it.fileFilterGroup.copy(selectVideo = checkState.isSelect)
                            FileType.AUDIO -> it.fileFilterGroup.copy(selectAudio = checkState.isSelect)
                            FileType.IMG -> it.fileFilterGroup.copy(selectImage = checkState.isSelect)
                            FileType.OTHER -> it.fileFilterGroup.copy(selectOther = checkState.isSelect)
                            FileType.ALL -> it.fileFilterGroup.copy(selectAll = checkState.isSelect)
                        }
                        dialogAccept(DialogAction.CalculateSizeInfo)
                        it.copy(
                            fileFilterGroup = filterGroup
                        )
                    } else {
                        it
                    }
                }
            }
        }

    private fun handleChangeDownloadPathFlow(setDownloadPathFlow: Flow<DialogAction.ChangeDownloadPath>) =
        viewModelScope.launch {
            setDownloadPathFlow.collect { changeDownloadPath ->
                if (_newTaskState.value is NewTaskState.PreparingData) {
                    _newTaskState.update {
                        (it as NewTaskState.PreparingData).copy(
                            task = it.task.update(
                                downloadPath = changeDownloadPath.downloadPath
                            )
                        )
                    }
                    dialogAccept(DialogAction.CalculateSizeInfo)
                }
            }
        }

    private fun handleCalculateSizeInfo(calculateSizeInfoFlow: Flow<DialogAction.CalculateSizeInfo>) =
        viewModelScope.launch {
            calculateSizeInfoFlow.collect {
                _newTaskState.update {
                    it as NewTaskState.PreparingData
                    val totalCheckedFileSize =
                        (it.task._fileTree as TreeNode.Directory).totalCheckedFileSize
                    val checkedFileCount = it.task._fileTree.checkedFileCount
                    val downloadPathFile = File(it.task._downloadPath)
                    if(!downloadPathFile.exists()){
                        downloadPathFile.mkdirs()
                    }
                    val freeSpace = downloadPathFile.freeSpace
                    val totalSpace = downloadPathFile.totalSpace
                    var downloadPathSizeInfo = application.getString(
                        R.string.download_size_info,
                        freeSpace.toHumanReading()
                    )
                    var freeSpaceState = FreeSpaceState.ENOUGH
                    if (freeSpace < 100.MB || freeSpace.toDouble() / totalSpace.toDouble() < 0.02) {
                        downloadPathSizeInfo =
                            "<font color=\"#FF9800\">${application.getString(R.string.free_space_shortage)}</font>"
                        freeSpaceState = FreeSpaceState.FREE_SPACE_SHORTAGE
                    }

                    if (freeSpace < totalCheckedFileSize) {
                        downloadPathSizeInfo =
                            "<font color=\"#FF1200\">${application.getString(R.string.free_space_not_enough)}</font>"
                        freeSpaceState = FreeSpaceState.NOT_ENOUGH_SPACE
                    }

                    it.copy(
                        taskSizeInfo = TaskSizeInfo(
                            taskSizeInfo = application.getString(
                                R.string.new_task_size_info,
                                checkedFileCount,
                                totalCheckedFileSize.toHumanReading()
                            ),
                            downloadPathSizeInfo = downloadPathSizeInfo,
                            freeSpaceState = freeSpaceState
                        )
                    )
                }
            }
        }

    private fun handleDownloadEngineFlow(changeDownloadEngineFlow: Flow<DialogAction.ChangeDownloadEngine>) =
        viewModelScope.launch {
            changeDownloadEngineFlow.collect { changeDownloadEngine ->
                if (_newTaskState.value is NewTaskState.PreparingData) {
                    _newTaskState.update {
                        (it as NewTaskState.PreparingData).copy(
                            task = it.task.update(
                                downloadEngine = changeDownloadEngine.engine
                            )
                        )
                    }
                }
            }
        }


    private fun initEmitToastAction(): (ToastAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<ToastAction> = MutableSharedFlow()
        val initToast = actionStateFlow.filterIsInstance<ToastAction.InitToast>()
        val emitToast = actionStateFlow.filterIsInstance<ToastAction.ShowToast>()

        handleInitToast(initToast)
        handleEmitToast(emitToast)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleInitToast(initToastFlow: Flow<ToastAction.InitToast>) =
        viewModelScope.launch {
            initToastFlow.collect {
                _toastState.update {
                    ToastState.INIT
                }
            }
        }

    private fun handleEmitToast(emitToastFlow: Flow<ToastAction.ShowToast>) =
        viewModelScope.launch {
            emitToastFlow.collect { emitToast ->
                _toastState.update {
                    ToastState.Show(emitToast.msg)
                }
                emitToast(ToastAction.InitToast)
            }
        }

    fun bindDownloadingTaskStatusMap(downloadingTaskStatusMap: MutableMap<String, TaskStatus>) {
        downloadingTaskStatusInService = WeakReference(downloadingTaskStatusMap)
    }

}

sealed class UiAction {
    data class InitTask(val url: String) : UiAction()
    data class StartDownloadTask(val engine: DownloadEngine) : UiAction()
    object SaveSession : UiAction()
}

sealed class DialogAction {
    object ResetAddUriDialogState : DialogAction()
    object ResetTaskSettingDialogState : DialogAction()

    data class FilterGroupState(val fileType: FileType, val isSelect: Boolean) : DialogAction()
    data class ChangeDownloadPath(val downloadPath: String) : DialogAction()
    data class ChangeDownloadEngine(val engine: DownloadEngine) : DialogAction()
    object CalculateSizeInfo : DialogAction()
}

sealed class ToastAction {
    object InitToast : ToastAction()
    data class ShowToast(val msg: String) : ToastAction()
}

data class MainUiState(
    val isLoading: Boolean = false,
    val isShowAddNewTask: Boolean = false,
)

sealed class ToastState {
    object INIT : ToastState()
    data class Show(val msg: String) : ToastState()
}

sealed class NewTaskState {
    object INIT : NewTaskState()
    data class PreparingData(
        val task: NewTaskConfigModel,
        val fileFilterGroup: fileFilterGroup = fileFilterGroup(),
        val taskSizeInfo: TaskSizeInfo = TaskSizeInfo()
    ) : NewTaskState()

    object Success : NewTaskState()

}

data class TaskSizeInfo(
    val taskSizeInfo: String = "",
    val downloadPathSizeInfo: String = "",
    val freeSpaceState: FreeSpaceState = FreeSpaceState.ENOUGH
)

data class fileFilterGroup(
    val selectAll: Boolean = false,
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
