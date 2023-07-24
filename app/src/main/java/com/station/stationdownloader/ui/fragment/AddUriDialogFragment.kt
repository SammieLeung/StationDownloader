package com.station.stationdownloader.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.R
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.databinding.DialogFragmentAddUriBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.contract.SelectFileActivityResultContract
import com.station.stationdownloader.ui.contract.SelectType
import com.station.stationdownloader.ui.viewmodel.AddUriUiState
import com.station.stationdownloader.ui.viewmodel.DialogAction
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
    private val stationPickerContract = SelectFileActivityResultContract(
        selectType = SelectType.SELECT_TYPE_FILE, showConfirmDialog = true
    )
    private val openStationPicker = registerForActivityResult(stationPickerContract) {
        if (it != null) {
            if (vm.assertTorrentFile(it.toString())) {
                mBinding.inputView.setText(it.toString())
            } else {
                mBinding.inputView.text = null
                Toast.makeText(context, R.string.assert_torrent_file, Toast.LENGTH_SHORT).show()
            }

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.addUriUiState,
            vm.accept,
        )
    }

    private fun DialogFragmentAddUriBinding.bindState(
        uiState: Flow<AddUriUiState<StationDownloadTask>>,
        accept: (UiAction) -> Unit,
    ) {
        inputView.setText("magnet:?xt=urn:btih:4edffd1faa7ca7422384147945a840ebfb5aa8b5&dn=Asteroid.City.2023.Retail.SWESUB.1080p.WEB.DDP5.1.Atmos.H.264-SiGGiZ")
        okBtn.setOnClickListener {
            done(accept)
        }
        cancelBtn.setOnClickListener {
            dismiss()
        }
        selectTorrentBtn.setOnClickListener {
            openStationPicker.launch(null)
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
                    Logger.d("dialog launch")
                    dismiss()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.dialogAccept(DialogAction.ResetAddUriDialogState)
    }

    private fun done(accept: (UiAction) -> Unit) {
        if (mBinding.inputView.text.toString().isNotEmpty())
            accept(UiAction.InitTask(mBinding.inputView.text.toString()))
    }


}