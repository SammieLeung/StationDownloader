package com.station.stationdownloader.ui.fragment.setting

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.DialogMaxThreadBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.utils.DLogger
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MaxThreadDialogFragment : BaseDialogFragment<DialogMaxThreadBinding>(), DLogger {

    private val vm by lazy {
        ViewModelProvider(parentFragment as ViewModelStoreOwner).get(SettingViewModel::class.java)
    }
    private val spinnerValues = listOf(1, 2, 3, 4, 5)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(vm.dialogState)
    }


    private fun DialogMaxThreadBinding.bindState(dialogState: StateFlow<DialogState>) {
        cancelBtn.setOnClickListener {
            dismiss()
        }
        okBtn.setOnClickListener {
            done(spinnerValues[maxThreadSpinner.selectedItemPosition])
        }
        maxThreadSpinner.adapter = StationSpinnerAdapter(requireContext(), spinnerValues)
        lifecycleScope.launch {
            dialogState.map { it.maxThread }
                .distinctUntilChanged()
                .collect {
                    val index = spinnerValues.indexOf(it)
                    maxThreadSpinner.setSelection(index)
                }
        }
    }

    private fun done(thread: Int) {
        vm.accept(UiAction.SetMaxThread(thread))
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        logger(vm)
        vm.accept(UiAction.ResetDialogState)
    }

    override fun DLogger.tag(): String {
        return MaxThreadDialogFragment::class.java.simpleName
    }
}

