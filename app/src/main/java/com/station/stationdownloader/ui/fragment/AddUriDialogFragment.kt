package com.station.stationdownloader.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.R
import com.station.stationdownloader.StationDownloaderApp
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.databinding.DialogFragmentAddUriBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.contract.OpenFileManagerV1Contract
import com.station.stationdownloader.ui.contract.OpenFileManagerV2Contract
import com.station.stationdownloader.ui.viewmodel.AddUriUiState
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.UiAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class AddUriDialogFragment : BaseDialogFragment<DialogFragmentAddUriBinding>() {
    val app: StationDownloaderApp by lazy {
        requireActivity().application as StationDownloaderApp
    }
    val vm: MainViewModel by activityViewModels<MainViewModel>()
    private val openStationV1Picker = registerForActivityResult(OpenFileManagerV1Contract()) {
        if (it != null) {
            if (vm.assertTorrentFile(it.toString())) {
                mBinding.inputView.setText(it.toString())
            } else {
                mBinding.inputView.text = null
                Toast.makeText(context, R.string.assert_torrent_file, Toast.LENGTH_SHORT).show()
            }

        }
    }

    private val openStationV2Picker =
        registerForActivityResult(OpenFileManagerV2Contract()) {
            it?.let { intent ->
                val dataType = intent.getIntExtra("data_type", -1)
                if (dataType == 1) {
                    val uri = intent.getStringExtra("path") ?: return@registerForActivityResult
                    if (vm.assertTorrentFile(uri)) {
                        mBinding.inputView.setText(uri)
                    } else {
                        mBinding.inputView.text = null
                        Toast.makeText(context, R.string.assert_torrent_file, Toast.LENGTH_SHORT)
                            .show()
                    }
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
        inputView.setText("magnet:?xt=urn:btih:2556a1cea083256b48caf891f7936be64af7c8ea&dn=%E4%BA%BA%E7%94%9F%E8%B7%AF%E4%B8%8D%E7%86%9F%5B%E6%9D%9C%E6%AF%94%E8%A7%86%E7%95%8C%E7%89%88%E6%9C%AC%5D%5B%E5%9B%BD%E8%AF%AD%E9%85%8D%E9%9F%B3%2B%E4%B8%AD%E6%96%87%E5%AD%97%E5%B9%95%5D.Godspeed.2023.2160p.TX.WEB-DL.H265.DV.DDP5.1-DreamHD")
        okBtn.setOnClickListener {
            done(accept)
        }
        cancelBtn.setOnClickListener {
            dismiss()
        }
        selectTorrentBtn.setOnClickListener {
            if (app.useV2FileManager) {
                openFileWithV2FileManager()
            } else {
                openFileWithV1FileManager()
            }
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


    private fun openFileWithV1FileManager() {
        openStationV1Picker.launch(Bundle().apply {
            putInt(
                OpenFileManagerV1Contract.EXTRA_SELECT_TYPE,
                OpenFileManagerV1Contract.SelectType.SELECT_TYPE_FILE.ordinal
            )
            putString(OpenFileManagerV1Contract.EXTRA_TITLE, getString(R.string.title_select_torrent_file))
            putBoolean(OpenFileManagerV1Contract.EXTRA_SUPPORT_NET, false)
            putBoolean(OpenFileManagerV1Contract.EXTRA_CONFIRM_DIALOG, true)
        })
    }

    private fun openFileWithV2FileManager() {
       openStationV2Picker.launch(Bundle().apply {
           putString(OpenFileManagerV2Contract.EXTRA_MIME_TYPE,"application/x-bittorrent")
        })
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