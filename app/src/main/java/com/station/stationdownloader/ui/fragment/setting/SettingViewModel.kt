package com.station.stationdownloader.ui.fragment.setting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.R
import com.station.stationdownloader.contants.Aria2Options
import com.station.stationdownloader.contants.CommonOptions
import com.station.stationdownloader.contants.Options
import com.station.stationdownloader.contants.XLOptions
import com.station.stationdownloader.data.source.repository.DefaultConfigurationRepository
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    val application: Application,
    val configRepo: DefaultConfigurationRepository,
    val engineRepo: DefaultEngineRepository
) :
    ViewModel(), DLogger {

    private val _commonSetting = MutableStateFlow(
        CommonSettingState(
            settingItemStates = listOf(
                SettingItemState(),
                SettingItemState(),
                SettingItemState()
            )
        )
    )
    val commonSetting = _commonSetting.asStateFlow()

    private val _xlSetting = MutableStateFlow(
        XLSettingState(
            settingItemStates = listOf(
                SettingItemState(),
            )
        )
    )
    val xlSetting = _xlSetting.asStateFlow()

    private val _aria2Setting = MutableStateFlow(
        Aria2SettingState(
            settingItemStates = listOf(
                SettingItemState(
                    title = application.getString(R.string.aria2_setting_status),
                    extraContent = "",
                    content = application.getString(
                        R.string.aria2_setting_status_obtaining
                    ),
                    onClick = {}),
                SettingItemState(),
            )
        )
    )
    val aria2Setting = _aria2Setting.asStateFlow()

    private val _dialogState = MutableStateFlow(
        DialogState(
            selectDownloadPath = false,
            selectMaxThread = false,
            selectSpeedLimit = false,
            selectEngine = false,
        )
    )
    val dialogState = _dialogState.asStateFlow()

    val accept: (UiAction) -> Unit

    init {
        accept = initAction()
    }

    private fun initAction(): (UiAction) -> Unit {

        val actionStateFlow: MutableSharedFlow<UiAction> = MutableSharedFlow()
        val refreshConfigurationsFlow =
            actionStateFlow.filterIsInstance<UiAction.RefreshConfigurations>()
        val updateDownloadEngineFlow =
            actionStateFlow.filterIsInstance<UiAction.UpdateDownloadEngine>()
        val updateDownloadPathFlow =
            actionStateFlow.filterIsInstance<UiAction.UpdateDownloadPath>()
        val updateMaxThreadFlow =
            actionStateFlow.filterIsInstance<UiAction.UpdateMaxThread>()
        val updateDownloadSpeedLimitFlow =
            actionStateFlow.filterIsInstance<UiAction.UpdateDownloadSpeedLimit>()

        val resetDialogStateFlow =
            actionStateFlow.filterIsInstance<UiAction.ResetDialogState>()

        val updateAria2StateFlow =
            actionStateFlow.filterIsInstance<UiAction.UpdateAria2State>()

        handleRefreshConfigurations(refreshConfigurationsFlow)
        handleUpdateDownloadEngine(updateDownloadEngineFlow)
        handleUpdateDownloadPath(updateDownloadPathFlow)
        handleUpdateMaxThread(updateMaxThreadFlow)
        handleUpdateDownloadSpeedLimit(updateDownloadSpeedLimitFlow)
        handleResetDialogState(resetDialogStateFlow)
        handleUpdateAria2UiState(updateAria2StateFlow)


        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleRefreshConfigurations(refreshFlow: Flow<UiAction.RefreshConfigurations>) =
        viewModelScope.launch {
            refreshFlow.collect {
                val downloadPath = configRepo.getValue(CommonOptions.DownloadPath)
                val maxThread = configRepo.getValue(CommonOptions.MaxThread).toInt()
                val defaultDownloadEngine =
                    DownloadEngine.valueOf(configRepo.getValue(CommonOptions.DefaultDownloadEngine))

                val xlDownloadSpeedLimit = configRepo.getValue(XLOptions.SpeedLimit).toLong()
                val aria2DownloadSpeedLimit = configRepo.getValue(Aria2Options.SpeedLimit).toLong()
                _commonSetting.update {
                    it.copy(
                        settingItemStates = listOf(
                            SettingItemState(
                                title = application.getString(R.string.common_setting_download_path),
                                extraContent = "",
                                content = downloadPath,
                                onClick = {
                                    viewModelScope.launch {
                                        _dialogState.update {
                                            it.copy(
                                                selectDownloadPath = true,
                                                selectMaxThread = false,
                                                selectSpeedLimit = false,
                                                selectEngine = false,
                                                downloadPath =  configRepo.getValue(CommonOptions.DownloadPath)
                                            )
                                        }
                                    }
                                }
                            ),
                            SettingItemState(
                                title = application.getString(R.string.common_setting_max_thread),
                                extraContent = "",
                                content = maxThread.toString(),
                                onClick = {
                                    viewModelScope.launch {
                                        _dialogState.update {
                                            it.copy(
                                                selectDownloadPath = false,
                                                selectMaxThread = true,
                                                selectSpeedLimit = false,
                                                selectEngine = false,
                                                maxThread = configRepo.getValue(CommonOptions.MaxThread).toInt()
                                            )
                                        }
                                    }
                                }
                            ),
                            SettingItemState(
                                title = application.getString(R.string.common_setting_default_download_engine),
                                extraContent = "",
                                content = defaultDownloadEngine.formatHumanName(),
                                onClick = {
                                    viewModelScope.launch {
                                        _dialogState.update {
                                            it.copy(
                                                selectDownloadPath = false,
                                                selectMaxThread = false,
                                                selectSpeedLimit = false,
                                                selectEngine = true,
                                                defaultDownloadEngine =    configRepo.getValue(CommonOptions.DefaultDownloadEngine)
                                            )
                                        }
                                    }
                                }
                            )

                        )
                    )
                }

                _xlSetting.update {
                    it.copy(
                        settingItemStates = listOf(
                            SettingItemState(
                                title = application.getString(R.string.xl_setting_download_speed_limit),
                                extraContent = "",
                                content = xlDownloadSpeedLimit.formatRoundSpeed(),
                                onClick = {
                                    viewModelScope.launch {
                                        _dialogState.update {
                                            it.copy(
                                                selectDownloadPath = false,
                                                selectMaxThread = false,
                                                selectSpeedLimit = true,
                                                selectEngine = false,
                                                speedLimitOption = XLOptions.SpeedLimit,
                                                downloadSpeedLimit = configRepo.getValue(XLOptions.SpeedLimit).toLong()
                                            )
                                        }
                                    }
                                }
                            )
                        )
                    )
                }

                _aria2Setting.update {
                    it.copy(
                        settingItemStates = listOf(
                            it.settingItemStates[0],
                            SettingItemState(
                                title = application.getString(R.string.aria2_setting_download_speed_limit),
                                extraContent = "",
                                content = aria2DownloadSpeedLimit.formatRoundSpeed(),
                                onClick = {
                                    viewModelScope.launch {
                                        _dialogState.update {
                                            it.copy(
                                                selectDownloadPath = false,
                                                selectMaxThread = false,
                                                selectSpeedLimit = true,
                                                selectEngine = false,
                                                speedLimitOption = Aria2Options.SpeedLimit,
                                                downloadSpeedLimit = configRepo.getValue(Aria2Options.SpeedLimit).toLong()
                                            )
                                        }
                                    }
                                }
                            )

                        )
                    )
                }
            }
        }

    private fun handleUpdateDownloadEngine(updateDownloadEngineFlow: Flow<UiAction.UpdateDownloadEngine>) {
        viewModelScope.launch {
            updateDownloadEngineFlow.collect { action ->
                engineRepo.changeOption(CommonOptions.DefaultDownloadEngine, action.engine.name)
                _commonSetting.update {
                    val list = it.settingItemStates.toMutableList()
                    list[2] = list[2].copy(content = action.engine.formatHumanName())
                    it.copy(settingItemStates = list.toList())
                }
            }
        }
    }

    private fun handleUpdateDownloadPath(updateDownloadPathFlow: Flow<UiAction.UpdateDownloadPath>) =
        viewModelScope.launch {
            updateDownloadPathFlow.collect { action ->
                engineRepo.changeOption(CommonOptions.DownloadPath, action.downloadPath)
                _commonSetting.update {
                    val list = it.settingItemStates.toMutableList()
                    list[0] = list[0].copy(content = action.downloadPath)
                    it.copy(settingItemStates = list.toList())
                }
            }
        }

    private fun handleUpdateMaxThread(updateMaxThreadFlow: Flow<UiAction.UpdateMaxThread>) =
        viewModelScope.launch {
            updateMaxThreadFlow.collect { action ->
                engineRepo.changeOption(CommonOptions.MaxThread, action.maxThread.toString())
                _commonSetting.update {
                    val list = it.settingItemStates.toMutableList()
                    list[1] = list[1].copy(content = action.maxThread.toString())
                    it.copy(settingItemStates = list.toList())
                }
            }
        }

    private fun handleUpdateDownloadSpeedLimit(updateDownloadSpeedLimitFlow: Flow<UiAction.UpdateDownloadSpeedLimit>) =
        viewModelScope.launch {
            updateDownloadSpeedLimitFlow.collect { action ->
                val options = dialogState.value.speedLimitOption
                engineRepo.changeOption(
                    options,
                    action.downloadSpeedLimit.toString()
                )
                if (options is Aria2Options.SpeedLimit) {
                    _aria2Setting.update { it ->
                        it.copy(
                            settingItemStates = listOf(
                                it.settingItemStates[0],
                                it.settingItemStates[1].copy(content = action.downloadSpeedLimit.formatRoundSpeed())
                            )
                        )
                    }
                } else if (options is XLOptions.SpeedLimit) {
                    _xlSetting.update {
                        it.copy(
                            settingItemStates = listOf(
                                it.settingItemStates[0].copy(content = action.downloadSpeedLimit.formatRoundSpeed())
                            )
                        )
                    }
                }
            }
        }

    private fun handleResetDialogState(resetDialogStateFlow: Flow<UiAction.ResetDialogState>) =
        viewModelScope.launch {
            resetDialogStateFlow.collect {
                _dialogState.update {
                    it.copy(
                        selectDownloadPath = false,
                        selectMaxThread = false,
                        selectSpeedLimit = false,
                        selectEngine = false,
                    )
                }
            }
        }

    private fun handleUpdateAria2UiState(updateAria2StateFlow: Flow<UiAction.UpdateAria2State>) {
        viewModelScope.launch {
            updateAria2StateFlow.collect { aria2State ->
                _aria2Setting.update {
                    logger("handleUpdateAria2UiState aria2Setting updating... ")

                    it.copy(
                        settingItemStates = listOf(
                            it.settingItemStates[0].copy(
                                content = aria2State.formatHumanName()
                            ),
                            it.settingItemStates[1]
                        )
                    )
                }
            }
        }
    }

    private fun UiAction.UpdateAria2State.formatHumanName(): String {
        return if (this.on) application.getString(R.string.aria2_setting_status_on) else application.getString(
            R.string.aria2_setting_status_off
        )
    }

    private fun Long.formatRoundSpeed(): String {
        if (this == 0L) return application.getString(R.string.speed_no_limit)
        return TaskTools.formatRoundSpeed(this)
    }

    private fun DownloadEngine.formatHumanName(): String {
        return when (this) {
            DownloadEngine.XL -> {
                application.getString(R.string.download_with_xl)
            }

            DownloadEngine.ARIA2 -> {
                application.getString(R.string.download_with_aria2)
            }

            else -> {
                application.getString(R.string.download_with_xl)
            }
        }
    }

    private fun getString(id: Int): String {
        return application.getString(id)
    }

    override fun DLogger.tag(): String {
        return SettingViewModel::class.java.simpleName
    }

}

data class DialogState(
    val selectDownloadPath: Boolean = false,
    val selectMaxThread: Boolean = false,
    val selectSpeedLimit: Boolean = false,
    val selectEngine: Boolean = false,
    val speedLimitOption: Options = XLOptions.SpeedLimit,
    val downloadPath: String = "",
    val maxThread: Int = 0,
    val downloadSpeedLimit: Long = 0L,
    val defaultDownloadEngine: String = "",
)

data class CommonSettingState(
    val settingItemStates: List<SettingItemState> = emptyList()
)

data class Aria2SettingState(
    val settingItemStates: List<SettingItemState> = emptyList()
)

data class XLSettingState(
    val settingItemStates: List<SettingItemState> = emptyList()
)

data class SettingItemState(
    val title: String = "",
    val extraContent: String = "",
    val content: String = "",
    val onClick: () -> Unit = {}
)

sealed class UiAction {
    object RefreshConfigurations : UiAction()
    object ResetDialogState : UiAction()
    data class UpdateDownloadEngine(val engine: DownloadEngine) : UiAction()
    data class UpdateDownloadPath(val downloadPath: String) : UiAction()
    data class UpdateMaxThread(val maxThread: Int) : UiAction()
    data class UpdateDownloadSpeedLimit(val downloadSpeedLimit: Long) :
        UiAction()

    data class UpdateAria2State(val on: Boolean) : UiAction()
}