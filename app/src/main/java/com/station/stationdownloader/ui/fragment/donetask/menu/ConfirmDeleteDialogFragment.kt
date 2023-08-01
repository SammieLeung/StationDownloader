package com.station.stationdownloader.ui.fragment.donetask.menu

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.station.stationdownloader.databinding.DialogConfirmDeleteBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.fragment.donetask.DownloadedTaskFragment
import com.station.stationdownloader.ui.fragment.donetask.UiAction
import com.station.stationdownloader.ui.fragment.downloading.menu.DoneTaskItemMenuDialogFragment.Companion.EXTRA_URL

class ConfirmDeleteDialogFragment private constructor(): BaseDialogFragment<DialogConfirmDeleteBinding>() {

    private val url by lazy {
        arguments?.getString(EXTRA_URL) ?: ""
    }
    private val vm by lazy {
        (requireParentFragment() as DownloadedTaskFragment).getViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(vm.accept)
    }

    fun DialogConfirmDeleteBinding.bindState(accept: (UiAction) -> Unit) {
        cancelBtn.setOnClickListener {
            dismiss()
        }
        okBtn.setOnClickListener {
            accept(UiAction.DeleteTask(url,deleteFileCheckBox.isChecked))
            dismiss()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.ShowDeleteConfirmDialog(false))
        vm.accept(UiAction.InitUiState)
    }

    companion object {
        fun newInstance(url: String): ConfirmDeleteDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_URL, url)
            val fragment = ConfirmDeleteDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}