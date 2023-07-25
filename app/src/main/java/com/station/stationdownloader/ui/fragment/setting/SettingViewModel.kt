package com.station.stationdownloader.ui.fragment.setting

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.station.stationdownloader.R
import com.station.stationdownloader.contants.DOWNLOAD_PATH
import com.station.stationdownloader.contants.MAX_THREAD
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    val application: Application,
    val configRepo: IConfigurationRepository,
    val engineRepo:IEngineRepository
) :
    ViewModel(), DLogger {

    private val _commonSetting = MutableStateFlow<CommonSettingState>(
        CommonSettingState(
            settingItemStates = listOf(
                SettingItemState(),
                SettingItemState(),
                SettingItemState()
            )
        )
    )
    val commonSetting = _commonSetting.asStateFlow()

    private val _dialogState = MutableStateFlow(
        DialogState(
            isShowDownloadPath = false,
            isShowMaxThread = false,
            isShowSpeedLimit = false
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
        val setDownloadPathFlow =
            actionStateFlow.filterIsInstance<UiAction.SetDownloadPath>()
        val setMaxThreadFlow =
            actionStateFlow.filterIsInstance<UiAction.SetMaxThread>()
        val setDownloadSpeedLimitFlow =
            actionStateFlow.filterIsInstance<UiAction.SetDownloadSpeedLimit>()

        val resetDialogStateFlow =
            actionStateFlow.filterIsInstance<UiAction.ResetDialogState>()

        handleRefreshConfigurations(refreshConfigurationsFlow)
        handleSetDownloadPath(setDownloadPathFlow)
        handleSetMaxThread(setMaxThreadFlow)
        handleSetDownloadSpeedLimit(setDownloadSpeedLimitFlow)
        handleResetDialogState(resetDialogStateFlow)

        return { action ->
            viewModelScope.launch {
                actionStateFlow.emit(action)
            }
        }
    }

    private fun handleRefreshConfigurations(refreshFlow: Flow<UiAction.RefreshConfigurations>) =
        viewModelScope.launch {
            refreshFlow.collect {
                val downloadPath = configRepo.getDownloadPath()
                val downloadSpeedLimit = configRepo.getSpeedLimit()
                val maxThread = configRepo.getMaxThread()
                _dialogState.update {
                    it.copy(
                        downloadPath = downloadPath,
                        downloadSpeedLimit = downloadSpeedLimit,
                        maxThread = maxThread
                    )
                }
                _commonSetting.update {
                    it.copy(
                        settingItemStates = listOf(
                            SettingItemState(
                                title = application.getString(R.string.common_setting_download_path),
                                extraContent = "",
                                content = downloadPath,
                                onClick = {
                                    _dialogState.update {
                                        it.copy(isShowDownloadPath = true)
                                    }
                                }
                            ),
                            SettingItemState(
                                title = application.getString(R.string.common_setting_max_thread),
                                extraContent = "",
                                content = maxThread.toString(),
                                onClick = {
                                    _dialogState.update {
                                        it.copy(isShowMaxThread = true)
                                    }
                                }
                            ),
                            SettingItemState(
                                title = application.getString(R.string.common_setting_download_speed_limit),
                                extraContent = "",
                                content = downloadSpeedLimit.formatRoundSpeed(),
                                onClick = {
                                    _dialogState.update {
                                        it.copy(isShowSpeedLimit = true)
                                    }
                                }
                            )

                        )
                    )
                }
            }
        }

    private fun handleSetDownloadPath(setDownloadPathFlow: Flow<UiAction.SetDownloadPath>) =
        viewModelScope.launch {
            setDownloadPathFlow.collect { action ->
                engineRepo.configure(DOWNLOAD_PATH,action.downloadPath)
                _commonSetting.update {
                    val list = it.settingItemStates.toMutableList()
                    list[0] = list[0].copy(content = action.downloadPath)
                    it.copy(settingItemStates = list.toList())
                }
            }
        }

    private fun handleSetMaxThread(setMaxThreadFlow: Flow<UiAction.SetMaxThread>) =
        viewModelScope.launch {
            setMaxThreadFlow.collect { action ->
              engineRepo.configure(MAX_THREAD,action.maxThread.toString())
                _commonSetting.update {
                    val list = it.settingItemStates.toMutableList()
                    list[1] = list[1].copy(content = action.maxThread.toString())
                    it.copy(settingItemStates = list.toList())
                }
            }
        }

    private fun handleSetDownloadSpeedLimit(setDownloadSpeedLimitFlow: Flow<UiAction.SetDownloadSpeedLimit>) =
        viewModelScope.launch {
            setDownloadSpeedLimitFlow.collect { action ->
              engineRepo.configure(SPEED_LIMIT,action.downloadSpeedLimit.toString())
                _commonSetting.update {
                    val list = it.settingItemStates.toMutableList()
                    list[2] = list[2].copy(content = action.downloadSpeedLimit.formatRoundSpeed())
                    it.copy(settingItemStates = list.toList())
                }
            }
        }

    private fun handleResetDialogState(resetDialogStateFlow: Flow<UiAction.ResetDialogState>) =
        viewModelScope.launch {
            resetDialogStateFlow.collect {
                _dialogState.update {
                    it.copy(
                        isShowDownloadPath = false,
                        isShowMaxThread = false,
                        isShowSpeedLimit = false
                    )
                }
            }
        }


    private fun Long.formatRoundSpeed(): String {
        if (this == -1L) return application.getString(R.string.speed_no_limit)
        return TaskTools.formatRoundSpeed(this)
    }

    override fun DLogger.tag(): String {
        return SettingViewModel::class.java.simpleName
    }

}

data class DialogState(
    val isShowDownloadPath: Boolean = false,
    val isShowMaxThread: Boolean = false,
    val isShowSpeedLimit: Boolean = false,
    val downloadPath: String = "",
    val maxThread: Int = 0,
    val downloadSpeedLimit: Long = 0L
)

data class CommonSettingState(
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
    data class SetDownloadPath(val downloadPath: String) : UiAction()
    data class SetMaxThread(val maxThread: Int) : UiAction()
    data class SetDownloadSpeedLimit(val downloadSpeedLimit: Long) : UiAction()
}