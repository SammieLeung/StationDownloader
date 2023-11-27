package com.station.stationdownloader.ui.fragment

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
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
import com.station.stationdownloader.ui.viewmodel.ToastAction
import com.station.stationdownloader.ui.viewmodel.UiAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
                mBinding.okBtn.requestFocus()
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
                        mBinding.okBtn.requestFocus()
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
            vm.emitToast
        )
    }

    private fun DialogFragmentAddUriBinding.bindState(
        uiState: Flow<AddUriUiState<StationDownloadTask>>,
        accept: (UiAction) -> Unit,
        emitToast: (ToastAction) -> Unit
    ) {
        okBtn.setOnClickListener {
            done(accept, emitToast)
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
                    inputView.shake()
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
            putString(
                OpenFileManagerV1Contract.EXTRA_TITLE,
                getString(R.string.title_select_torrent_file)
            )
            putBoolean(OpenFileManagerV1Contract.EXTRA_SUPPORT_NET, false)
            putBoolean(OpenFileManagerV1Contract.EXTRA_CONFIRM_DIALOG, true)
        })
    }

    private fun openFileWithV2FileManager() {
        openStationV2Picker.launch(Bundle().apply {
            putString(OpenFileManagerV2Contract.EXTRA_MIME_TYPE, "application/x-bittorrent")
        })
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.dialogAccept(DialogAction.ResetAddUriDialogState)
    }

    private fun done(accept: (UiAction) -> Unit, emitToast: (ToastAction) -> Unit) {
        if (mBinding.inputView.text.toString().isNotEmpty())
            accept(UiAction.InitTask(mBinding.inputView.text.toString()))
        else {
            mBinding.inputView.shake()
            emitToast(ToastAction.ShowToast(requireContext().getString(R.string.uri_is_empty)))
        }
    }

    private fun View.shake() {
            AnimatorInflater.loadAnimator(context, R.animator.view_shake).apply {
                setTarget(this@shake)
                start()
            }
    }
}