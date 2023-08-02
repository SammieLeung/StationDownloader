package com.station.stationdownloader.ui.fragment.setting

import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.R
import com.station.stationdownloader.StationDownloaderApp
import com.station.stationdownloader.databinding.FragmentSettingsBinding
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.contract.OpenFileManagerV1Contract
import com.station.stationdownloader.ui.contract.OpenFileManagerV2Contract
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : BaseFragment<FragmentSettingsBinding>(), DLogger {
    private val app by lazy {
        requireActivity().application as StationDownloaderApp
    }
    private val vm by viewModels<SettingViewModel>()
    private val openStationV1 = registerForActivityResult(OpenFileManagerV1Contract()) {
        if (it != null) {
            val base64Id: String = it.pathSegments[1] //dir id
            val decodeData = String(Base64.decode(base64Id, Base64.DEFAULT))
            vm.accept(UiAction.SetDownloadPath(decodeData))
        }
        vm.accept(UiAction.ResetDialogState)
    }

    private val openFolderV2 = registerForActivityResult(OpenFileManagerV2Contract()) {
        it?.let { intent ->
            val dataType = intent.getIntExtra("data_type", -1)
            if (dataType == 2) {
                val uri = intent.getStringExtra("path") ?: return@registerForActivityResult
                vm.accept(UiAction.SetDownloadPath(uri))
            }
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
        if(app.useV2FileManager){
            openFileManagerV2()
        }else{
            openFileManagerV1()
        }
    }

    private fun openFileManagerV1() {
        openStationV1.launch(Bundle().apply {
            putInt(
                OpenFileManagerV1Contract.EXTRA_SELECT_TYPE,
                OpenFileManagerV1Contract.SelectType.SELECT_TYPE_FOLDER.type
            )
            putBoolean(OpenFileManagerV1Contract.EXTRA_SUPPORT_NET, false)
            putString(
                OpenFileManagerV1Contract.EXTRA_TITLE,
                getString(R.string.title_select_download_path)
            )
            putBoolean(OpenFileManagerV1Contract.EXTRA_CONFIRM_DIALOG, true)
        })
    }

    private fun openFileManagerV2() {
        openFolderV2.launch(Bundle().apply {
            putString(OpenFileManagerV2Contract.EXTRA_MIME_TYPE,"folder/*")
        })
    }


    override fun DLogger.tag(): String {
        return SettingsFragment::class.java.simpleName
    }

}