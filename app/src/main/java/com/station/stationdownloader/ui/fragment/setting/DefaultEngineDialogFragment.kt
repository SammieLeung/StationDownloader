package com.station.stationdownloader.ui.fragment.setting

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.R
import com.station.stationdownloader.databinding.DialogDefaultEngineBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.utils.DLogger
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DefaultEngineDialogFragment : BaseDialogFragment<DialogDefaultEngineBinding>(),DLogger {

    private val vm by lazy {
        ViewModelProvider(parentFragment as ViewModelStoreOwner).get(SettingViewModel::class.java)
    }
    private val spinnerData by lazy {
        listOf(
            resources.getString(R.string.download_with_xl),
            resources.getString(R.string.download_with_aria2)
        )
    }

    private val spinnerValues = listOf(DownloadEngine.XL, DownloadEngine.ARIA2)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.dialogState
        )
    }


    private fun DialogDefaultEngineBinding.bindState(dialogState: StateFlow<DialogState>) {
        cancelBtn.setOnClickListener {
            dismiss()
        }
        okBtn.setOnClickListener {
            done(spinnerValues[downloadEngineSpinner.selectedItemPosition])
        }

        downloadEngineSpinner.adapter = StationSpinnerAdapter(requireContext(), spinnerData)

        lifecycleScope.launch {
            dialogState.map { it.defaultDownloadEngine }
                .distinctUntilChanged()
                .collect {
                    logger(it)
                    val index = spinnerValues.indexOf(DownloadEngine.valueOf(it))
                    downloadEngineSpinner.setSelection(index)
                }
        }
    }

    private fun done(selectDownloadEngine: DownloadEngine) {
        vm.accept(
            UiAction.UpdateDownloadEngine(selectDownloadEngine)
        )
        dismiss()
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.ResetDialogState)
    }

    override fun DLogger.tag(): String {
        return "DefaultEngineDialogFragment"
    }

}

