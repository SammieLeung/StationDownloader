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
import com.station.stationdownloader.data.MultiTaskResult
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.engine.MultiNewTaskConfigModel
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
import kotlinx.coroutines.cancel
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
        val initTask = actionStateFlow.filterIsInstance<UiAction.InitSingleTask>()
        val initMultiTask = actionStateFlow.filterIsInstance<UiAction.InitMultiTask>()
        val startTask = actionStateFlow.filterIsInstance<UiAction.StartDownloadTask>()
        val saveSession = actionStateFlow.filterIsInstance<UiAction.SaveSession>()

        handleInitSingleTaskAction(initTask)
        handleInitMultiTaskAction(initMultiTask)
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

                when (_newTaskState.value) {
                    is NewTaskState.PreparingData -> startSingleDownloadTask(action)
                    is NewTaskState.PreparingMultiData -> startMultiDownloadTask(action)
                    else -> return@collect
                }
            }
        }


    private suspend fun startSingleDownloadTask(action: UiAction.StartDownloadTask) {
        val preparingData = _newTaskState.value as NewTaskState.PreparingData

        val saveTaskResult =
            taskRepo.validateAndPersistTask(
                preparingData.singleNewTaskConfig
            )
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
            return
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


    private suspend fun startMultiDownloadTask(action: UiAction.StartDownloadTask) {
        val preparingMultiData = _newTaskState.value as NewTaskState.PreparingMultiData

        val failedTaskConfigs: MutableList<Pair<String, IResult.Error>> = mutableListOf()
        preparingMultiData.multiNewTaskConfig.taskConfigs.forEach {
            val saveTaskResult =
                taskRepo.validateAndPersistTask(it)
            if (saveTaskResult is IResult.Error) {
                failedTaskConfigs.add(Pair(it._name, saveTaskResult))
            } else {
                saveTaskResult as IResult.Success
                TaskService.startTask(application, saveTaskResult.data.url)
            }
        }

        _toastState.update {
            ToastState.Show(application.getString(R.string.start_to_download))
        }
        _newTaskState.update {
            NewTaskState.Success
        }
    }


    private fun handleInitSingleTaskAction(initTaskFlow: Flow<UiAction.InitSingleTask>) =
        viewModelScope.launch {
            initTaskFlow.flatMapLatest {
                flow {
                    _addUriState.update {
                        AddUriUiState.LOADING
                    }
                    _mainUiState.update {
                        it.isLoading()
                    }
                    val result = engineRepo.initUrl(it.url)
                    emit(result)
                }
            }.collect { result ->
                _mainUiState.update {
                    it.hideLoading()
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
                        singleNewTaskConfig = newTaskModel,
                    )
                }
                _mainUiState.update {
                    it.showAddNewTask()
                }
                dialogAccept(DialogAction.CalculateSizeInfo)
            }

        }

    private fun handleInitMultiTaskAction(initMultiTaskFlow: Flow<UiAction.InitMultiTask>) =
        viewModelScope.launch {
            initMultiTaskFlow.collect {
                _addUriState.update {
                    AddUriUiState.LOADING
                }
                _mainUiState.update {
                    it.isLoading()
                }
                initMultiUrlWithProgress(it.urlList)
//                    initMultiUrlWithoutProgress(it.urlList)
            }
        }

    private fun initMultiUrlWithProgress(urlList: List<String>) = viewModelScope.launch {
        engineRepo.initMultiUrlFlow(urlList)
            .collect { multiTaskResult ->
                when (multiTaskResult) {
                    is MultiTaskResult.Begin -> {
                        logLine("Begin")
                        _addUriState.update {
                            AddUriUiState.SUCCESS
                        }
                        _mainUiState.update {
                            it.hideLoading()
                        }
                        _mainUiState.update {
                            it.showAddMultiTask()
                        }
                        _newTaskState.update {
                            NewTaskState.PreparingMultiData(
                                multiNewTaskConfig = multiTaskResult.multiNewTaskConfigModel
                            )
                        }
                    }

                    is MultiTaskResult.InitializingTask -> {
                        _newTaskState.update {
                            if (it is NewTaskState.PreparingMultiData) {
                                it.copy(initializingUrl = multiTaskResult.url)
                            } else it
                        }
                    }

                    MultiTaskResult.Finish -> {
                        logLine("Finish")

                        _newTaskState.update {
                            if (it is NewTaskState.PreparingMultiData) {
                                it.copy(
                                    isPrepared = true
                                )
                            } else it
                        }
                        dialogAccept(DialogAction.CalculateSizeInfo)
                        this.cancel()
                    }

                    else -> {}
                }
            }
    }

    private suspend fun initMultiUrlWithoutProgress(urlList: List<String>) {
        _addUriState.update {
            AddUriUiState.LOADING
        }
        _mainUiState.update {
            it.isLoading()
        }
        val multiConfigResult = engineRepo.initMultiUrl(urlList)
        _mainUiState.update {
            it.hideLoading()
        }
        if (multiConfigResult is IResult.Error) {
            _addUriState.update {
                AddUriUiState.ERROR(multiConfigResult.exception.message.toString())
            }
            Logger.e(multiConfigResult.exception.message.toString())
            return
        }

        _addUriState.update {
            AddUriUiState.SUCCESS
        }

        val multiTaskConfigs = (multiConfigResult as IResult.Success).data
        _newTaskState.update {
            NewTaskState.PreparingMultiData(
                isPrepared = true,
                multiNewTaskConfig = multiTaskConfigs
            )
        }
        _mainUiState.update {
            it.showAddMultiTask()
        }
        dialogAccept(DialogAction.CalculateSizeInfo)
    }

    private fun initAddUriDialogAcceptAction(): (DialogAction) -> Unit {
        val actionStateFlow: MutableSharedFlow<DialogAction> = MutableSharedFlow()
        val initAddUriState =
            actionStateFlow.filterIsInstance<DialogAction.ResetAddUriDialogState>()
        val hideAllDialogFlow =
            actionStateFlow.filterIsInstance<DialogAction.ReinitializeAllDialogAction>()
        val filterStateFlow = actionStateFlow.filterIsInstance<DialogAction.FilterGroupState>()

        val changeDownloadPathFlow =
            actionStateFlow.filterIsInstance<DialogAction.ChangeDownloadPath>()
        val changeDownloadEngineFlow =
            actionStateFlow.filterIsInstance<DialogAction.ChangeDownloadEngine>()
        val calculateSizeInfoFlow =
            actionStateFlow.filterIsInstance<DialogAction.CalculateSizeInfo>()
        val openMultiTaskDetailFlow =
            actionStateFlow.filterIsInstance<DialogAction.ShowMultiTaskDetail>()
        val addMultiTaskDialog = actionStateFlow.filterIsInstance<DialogAction.BackToAddMultiTask>()

        handleInitAddUriState(initAddUriState)
        handleReinitializeAllDialog(hideAllDialogFlow)
        handleOpenMultiTaskDetail(openMultiTaskDetailFlow)
        handleFilterState(filterStateFlow)
        handleChangeDownloadPathFlow(changeDownloadPathFlow)
        handleCalculateSizeInfo(calculateSizeInfoFlow)
        handleDownloadEngineFlow(changeDownloadEngineFlow)
        handleAddMultiTaskDialogFlow(addMultiTaskDialog)

        return { dialogAction ->
            viewModelScope.launch {
                actionStateFlow.emit(dialogAction)
            }
        }
    }

    private fun handleAddMultiTaskDialogFlow(addMultiTaskDialog: Flow<DialogAction.BackToAddMultiTask>) =
        viewModelScope.launch {
            addMultiTaskDialog.collect {
                _mainUiState.update {
                    it.showAddMultiTask()
                }
            }
        }


    private fun handleInitAddUriState(initAddUriStateFlow: Flow<DialogAction.ResetAddUriDialogState>) =
        viewModelScope.launch {
            initAddUriStateFlow.collect {
                _addUriState.value = AddUriUiState.INIT
            }
        }

    private fun handleReinitializeAllDialog(initTaskSettingStateFlow: Flow<DialogAction.ReinitializeAllDialogAction>) =
        viewModelScope.launch {
            initTaskSettingStateFlow.collect {
                _newTaskState.value = NewTaskState.INIT
                _mainUiState.update {
                    it.reinitializeAllDialog()
                }
            }
        }

    private fun handleOpenMultiTaskDetail(openMultiTaskDetailFlow: Flow<DialogAction.ShowMultiTaskDetail>) =
        viewModelScope.launch {
            openMultiTaskDetailFlow.collect {
                _mainUiState.update {
                    it.showMultiTaskDetail()
                }
            }
        }


    private fun handleFilterState(filterStateFlow: Flow<DialogAction.FilterGroupState>) =
        viewModelScope.launch {
            filterStateFlow.collect { checkState ->
                logger("actionCheckStateFlow collect")
                when (_newTaskState.value) {
                    is NewTaskState.PreparingData -> {
                        updateSingleFileTreeAndFileGroup(checkState)
                    }

                    is NewTaskState.PreparingMultiData -> {
                        updateMultiFileTreeAndFileGroup(checkState)
                    }

                    else -> return@collect
                }

            }
        }

    private fun updateSingleFileTreeAndFileGroup(checkState: DialogAction.FilterGroupState) {
        _newTaskState.update {
            it as NewTaskState.PreparingData
            it.singleNewTaskConfig._fileTree as TreeNode.Directory
            it.singleNewTaskConfig._fileTree.filterFile(
                checkState.fileType,
                checkState.isSelect
            )
            val filterGroup = when (checkState.fileType) {
                FileType.ALL -> it.fileFilterGroup.selectAll(checkState.isSelect)
                FileType.VIDEO -> it.fileFilterGroup.selectVideo(checkState.isSelect)
                FileType.AUDIO -> it.fileFilterGroup.selectAudio(checkState.isSelect)
                FileType.IMG ->it.fileFilterGroup.selectImage(checkState.isSelect)
                FileType.OTHER -> it.fileFilterGroup.selectOther(checkState.isSelect)
            }

            dialogAccept(DialogAction.CalculateSizeInfo)
            it.copy(
                fileFilterGroup = filterGroup
            )
        }
    }


    private fun updateMultiFileTreeAndFileGroup(checkState: DialogAction.FilterGroupState) {
        _newTaskState.update {
            it as NewTaskState.PreparingMultiData
            it.multiNewTaskConfig.filterFile(checkState.fileType, checkState.isSelect)
            val filterGroup = when (checkState.fileType) {
                FileType.ALL -> it.fileFilterGroup.selectAll(checkState.isSelect)
                FileType.VIDEO -> it.fileFilterGroup.selectVideo(checkState.isSelect)
                FileType.AUDIO -> it.fileFilterGroup.selectAudio(checkState.isSelect)
                FileType.IMG ->it.fileFilterGroup.selectImage(checkState.isSelect)
                FileType.OTHER -> it.fileFilterGroup.selectOther(checkState.isSelect)
            }
            dialogAccept(DialogAction.CalculateSizeInfo)
            it.copy(
                fileFilterGroup = filterGroup
            )
        }
    }


    private fun handleChangeDownloadPathFlow(setDownloadPathFlow: Flow<DialogAction.ChangeDownloadPath>) =
        viewModelScope.launch {
            setDownloadPathFlow.collect { changeDownloadPath ->
                when (_newTaskState.value) {
                    is NewTaskState.PreparingData -> changeSingleDownloadPath(changeDownloadPath)
                    is NewTaskState.PreparingMultiData -> changeMultiDownloadPath(
                        changeDownloadPath
                    )

                    else -> return@collect
                }
            }
        }

    private fun changeSingleDownloadPath(changeDownloadPath: DialogAction.ChangeDownloadPath) {
        _newTaskState.update {
            (it as NewTaskState.PreparingData).copy(
                singleNewTaskConfig = it.singleNewTaskConfig.update(
                    downloadPath = changeDownloadPath.downloadPath
                )
            )
        }
        dialogAccept(DialogAction.CalculateSizeInfo)
    }

    private fun changeMultiDownloadPath(changeDownloadPath: DialogAction.ChangeDownloadPath) {
        _newTaskState.update {
            it as NewTaskState.PreparingMultiData
            it.copy(
                multiNewTaskConfig = it.multiNewTaskConfig.update(
                    downloadPath = changeDownloadPath.downloadPath
                ),
            )
        }
        dialogAccept(DialogAction.CalculateSizeInfo)
    }

    private fun handleCalculateSizeInfo(calculateSizeInfoFlow: Flow<DialogAction.CalculateSizeInfo>) =
        viewModelScope.launch {
            calculateSizeInfoFlow.collect {
                when (_newTaskState.value) {
                    is NewTaskState.PreparingData -> calculatePreparingDataSizeInfo()
                    is NewTaskState.PreparingMultiData -> calculatePreparingMultiDataSizeInfo()
                    else -> {
                        logLine("handleCalculateSizeInfo error: Unsupport ${_newTaskState.value}")
                    }
                }

            }
        }

    private fun calculatePreparingDataSizeInfo() {
        _newTaskState.update {
            it as NewTaskState.PreparingData
            val totalCheckedFileSize =
                (it.singleNewTaskConfig._fileTree as TreeNode.Directory).totalCheckedFileSize
            val checkedFileCount = it.singleNewTaskConfig._fileTree.checkedFileCount
            val downloadPathFile = File(it.singleNewTaskConfig._downloadPath)
            if (!downloadPathFile.exists()) {
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

    private fun calculatePreparingMultiDataSizeInfo() {
        _newTaskState.update { newTaskState ->
            newTaskState as NewTaskState.PreparingMultiData
            val totalSelectedFileSize =
                newTaskState.multiNewTaskConfig.taskConfigs.sumOf { taskConfig ->
                    (taskConfig._fileTree as TreeNode.Directory).totalCheckedFileSize
                }
            val selectedFileCount =
                newTaskState.multiNewTaskConfig.taskConfigs.sumOf { taskConfig ->
                    (taskConfig._fileTree as TreeNode.Directory).checkedFileCount
                }

            val downloadPathFile = File(newTaskState.multiNewTaskConfig.downloadPath)
            if (!downloadPathFile.exists()) {
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

            if (freeSpace < totalSelectedFileSize) {
                downloadPathSizeInfo =
                    "<font color=\"#FF1200\">${application.getString(R.string.free_space_not_enough)}</font>"
                freeSpaceState = FreeSpaceState.NOT_ENOUGH_SPACE
            }

            newTaskState.copy(
                taskSizeInfo = TaskSizeInfo(
                    taskSizeInfo = application.getString(
                        R.string.new_task_size_info,
                        selectedFileCount,
                        totalSelectedFileSize.toHumanReading()
                    ),
                    downloadPathSizeInfo = downloadPathSizeInfo,
                    freeSpaceState = freeSpaceState
                )
            )
        }
    }

    private fun handleDownloadEngineFlow(changeDownloadEngineFlow: Flow<DialogAction.ChangeDownloadEngine>) =
        viewModelScope.launch {
            changeDownloadEngineFlow.collect { changeDownloadEngine ->
                when (_newTaskState.value) {
                    is NewTaskState.PreparingData -> changeSingleDownloadEngine(
                        changeDownloadEngine
                    )

                    is NewTaskState.PreparingMultiData -> changeMultiDownloadEngine(
                        changeDownloadEngine
                    )

                    else -> return@collect
                }
                if (_newTaskState.value is NewTaskState.PreparingData) {
                    _newTaskState.update {
                        (it as NewTaskState.PreparingData).copy(
                            singleNewTaskConfig = it.singleNewTaskConfig.update(
                                downloadEngine = changeDownloadEngine.engine
                            )
                        )
                    }
                }
            }
        }

    private fun changeSingleDownloadEngine(changeDownloadEngine: DialogAction.ChangeDownloadEngine) {
        _newTaskState.update {
            (it as NewTaskState.PreparingData).copy(
                singleNewTaskConfig = it.singleNewTaskConfig.update(
                    downloadEngine = changeDownloadEngine.engine
                )
            )
        }
    }

    private fun changeMultiDownloadEngine(changeDownloadEngine: DialogAction.ChangeDownloadEngine) {
        _newTaskState.update {
            (it as NewTaskState.PreparingMultiData).copy(
                multiNewTaskConfig = it.multiNewTaskConfig.update(
                    downloadEngine = changeDownloadEngine.engine
                )
            )
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
    data class InitSingleTask(val url: String) : UiAction()
    data class InitMultiTask(val urlList: List<String>) : UiAction()
    object StartDownloadTask : UiAction()
    object SaveSession : UiAction()
}

sealed class DialogAction {
    object ResetAddUriDialogState : DialogAction()
    object ReinitializeAllDialogAction : DialogAction()

    data class FilterGroupState(val fileType: FileType, val isSelect: Boolean) : DialogAction()
    data class ChangeDownloadPath(val downloadPath: String) : DialogAction()
    data class ChangeDownloadEngine(val engine: DownloadEngine) : DialogAction()
    object ShowMultiTaskDetail : DialogAction()
    object BackToAddMultiTask : DialogAction()
    object CalculateSizeInfo : DialogAction()
}

sealed class ToastAction {
    object InitToast : ToastAction()
    data class ShowToast(val msg: String) : ToastAction()
}

data class MainUiState(
    val isLoading: Boolean = false,
    val isShowAddNewTask: Boolean = false,
    val isShowAddMultiTask: Boolean = false,
    val isShowMultiTaskDetail: Boolean = false,
) {
    fun isLoading(): MainUiState =
        copy(isLoading = true)

    fun hideLoading(): MainUiState =
        copy(isLoading = false)

    fun showAddNewTask(): MainUiState =
        copy(isShowAddNewTask = true, isShowAddMultiTask = false, isShowMultiTaskDetail = false)


    fun showAddMultiTask(): MainUiState =
        copy(isShowAddNewTask = false, isShowAddMultiTask = true, isShowMultiTaskDetail = false)

    fun showMultiTaskDetail(): MainUiState =
        copy(isShowAddNewTask = false, isShowAddMultiTask = false, isShowMultiTaskDetail = true)

    fun reinitializeAllDialog(): MainUiState =
        copy(
            isShowAddNewTask = false,
            isShowAddMultiTask = false,
            isShowMultiTaskDetail = false
        )

}

sealed class ToastState {
    object INIT : ToastState()
    data class Show(val msg: String) : ToastState()
}

sealed class NewTaskState {
    object INIT : NewTaskState()
    data class PreparingData(
        val singleNewTaskConfig: NewTaskConfigModel,
        val fileFilterGroup: fileFilterGroup = fileFilterGroup(),
        val taskSizeInfo: TaskSizeInfo = TaskSizeInfo()
    ) : NewTaskState()

    data class PreparingMultiData(
        val isPrepared: Boolean = false,
        val initializingUrl: String? = null,
        val multiNewTaskConfig: MultiNewTaskConfigModel,
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
) {
    fun selectAll(isSelect: Boolean): fileFilterGroup {
        return copy(
            selectAll = isSelect,
            selectVideo = isSelect,
            selectAudio = isSelect,
            selectImage = isSelect,
            selectOther = isSelect,
        )
    }

    fun selectVideo(isSelect: Boolean): fileFilterGroup {
        return copy(
            selectVideo = isSelect,
            selectAll = isSelect && selectAudio && selectImage && selectOther
        )
    }

    fun selectAudio(isSelect: Boolean): fileFilterGroup {
        return copy(
            selectAudio = isSelect,
            selectAll = isSelect && selectVideo && selectImage && selectOther
        )
    }

    fun selectImage(isSelect: Boolean): fileFilterGroup {
        return copy(
            selectImage = isSelect,
            selectAll = isSelect && selectVideo && selectAudio && selectOther
        )
    }

    fun selectOther(isSelect: Boolean): fileFilterGroup {
        return copy(
            selectOther = isSelect,
            selectAll = isSelect && selectVideo && selectAudio && selectImage
        )
    }

}


sealed class AddUriUiState<out T> {
    object INIT : AddUriUiState<Nothing>()
    object LOADING : AddUriUiState<Nothing>()
    object SUCCESS : AddUriUiState<Nothing>()
    data class ERROR(val errMsg: String = "") : AddUriUiState<Nothing>()
}
