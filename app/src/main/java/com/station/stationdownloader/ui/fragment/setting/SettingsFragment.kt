package com.station.stationdownloader.ui.fragment.setting

import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.FragmentSettingsBinding
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.contract.SelectFileActivityResultContract
import com.station.stationdownloader.ui.contract.SelectType
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(), DLogger {

    private val vm by viewModels<SettingViewModel>()
    private val stationPickerContract = SelectFileActivityResultContract(
        selectType = SelectType.SELECT_TYPE_FOLDER, showConfirmDialog = true
    )
    private val openStationPicker = registerForActivityResult(stationPickerContract) {
        if (it != null) {
            val base64Id: String = it.pathSegments[1] //dir id
            val decodeData = String(Base64.decode(base64Id, Base64.DEFAULT))
            vm.accept(UiAction.SetDownloadPath(decodeData))
        }
        vm.accept(UiAction.ResetDialogState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger(vm)
        mBinding.bindState(
            vm.commonSetting,
            vm.dialogState,
            vm.accept
        )
    }

    override fun onResume() {
        super.onResume()
        vm.accept(UiAction.RefreshConfigurations)
    }

    private fun FragmentSettingsBinding.bindState(
        commonSetting: StateFlow<CommonSettingState>,
        dialogState: StateFlow<DialogState>,
        accept: (UiAction) -> Unit
    ) {


        lifecycleScope.launch {
            commonSetting.collect { settingState ->
                itemStateList = settingState.settingItemStates

                commonSettingDownloadPath.root.setOnClickListener {
                    settingState.settingItemStates[0].onClick()
                }
                commonSettingSimultaneousDownloadTasks.root.setOnClickListener {
                    settingState.settingItemStates[1].onClick()
                }
                commonSettingDownloadSpeedLimit.root.setOnClickListener {
                    settingState.settingItemStates[2].onClick()
                }

            }
        }

        lifecycleScope.launch {
            dialogState.collect {
                if (it.isShowDownloadPath) showDownloadPathDialog()
                else if (it.isShowMaxThread) showMaxThreadDialog()
                else if (it.isShowSpeedLimit) showSpeedLimitDialog()
            }
        }
    }

    private fun showMaxThreadDialog() {
        val dialog = MaxThreadDialogFragment()
        dialog.show(childFragmentManager, "")
    }

    private fun showSpeedLimitDialog() {
        val dialog = SpeedLimitDialogFragment()
        dialog.show(childFragmentManager, "")
    }

    private fun showDownloadPathDialog() {
        openStationPicker.launch(null)
    }

    override fun DLogger.tag(): String {
        return SettingsFragment::class.java.simpleName
    }

}