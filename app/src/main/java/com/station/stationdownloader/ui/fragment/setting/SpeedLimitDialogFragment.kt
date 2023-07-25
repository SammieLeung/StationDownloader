package com.station.stationdownloader.ui.fragment.setting

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.R
import com.station.stationdownloader.databinding.DialogSpeedLimitBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.utils.KB
import com.station.stationdownloader.utils.MB
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SpeedLimitDialogFragment : BaseDialogFragment<DialogSpeedLimitBinding>() {

    private val vm by lazy {
        ViewModelProvider(parentFragment as ViewModelStoreOwner).get(SettingViewModel::class.java)
    }
    private val spinnerData by lazy {
        listOf(
            resources.getString(R.string.speed_no_limit),
            "500KB/S",
            "1MB/S",
            "2MB/S",
            "5MB/S",
            "10MB/S",
        )
    }

    private val spinnerValues = listOf<Long>(
        -1,
        500.KB.toLong(),
        1.MB.toLong(),
        2.MB.toLong(),
        5.MB.toLong(),
        10.MB.toLong(),
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.dialogState
        )
    }


    private fun DialogSpeedLimitBinding.bindState(dialogState: StateFlow<DialogState>) {
        cancelBtn.setOnClickListener {
            dismiss()
        }
        okBtn.setOnClickListener{
            done(spinnerValues[speedLimitSpinner.selectedItemPosition])
        }

        speedLimitSpinner.adapter = StationSpinnerAdapter(requireContext(), spinnerData)

        lifecycleScope.launch {
            dialogState.map { it.downloadSpeedLimit }
                .distinctUntilChanged()
                .collect {
                    val index = spinnerValues.indexOf(it)
                    speedLimitSpinner.setSelection(index)
                }
        }
    }

    private fun done(speedLimit: Long) {
        vm.accept(UiAction.SetDownloadSpeedLimit(speedLimit))
        dismiss()
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.ResetDialogState)
    }

}

