package com.station.stationdownloader.ui.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.station.stationdownloader.databinding.DialogFragmentAddUriBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.AddUriUiState
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.UiAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class AddUriDialogFragment : BaseDialogFragment<DialogFragmentAddUriBinding>() {
    val vm: MainViewModel by activityViewModels<MainViewModel>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d("$vm")
        mBinding.bindState(
            vm.addUriUiState,
            vm.accept
        )
    }

    private fun DialogFragmentAddUriBinding.bindState(
        uiState: Flow<AddUriUiState<StationDownloadTask>>,
        accept: (UiAction) -> Unit
    ) {
        okBtn.setOnClickListener {
            done(accept)
        }
        cancelBtn.setOnClickListener {
            dismiss()
        }

        lifecycleScope.launch {
            uiState.collect {
                if (it is AddUriUiState.LOADING) {
                    this@AddUriDialogFragment.dialog?.hide()
                } else if (it is AddUriUiState.ERROR) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, it.errMsg, Toast.LENGTH_SHORT).show()
                    }
                    this@AddUriDialogFragment.dialog?.show()
                } else if (it is AddUriUiState.SUCCESS) {
                    dismiss()
                }
            }
        }
    }

    private fun done(accept: (UiAction) -> Unit) {
        accept(UiAction.InitTask(mBinding.inputView.text.toString()))
    }


}