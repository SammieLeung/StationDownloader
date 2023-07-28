package com.station.stationdownloader.ui.fragment.downloading.menu

import android.content.DialogInterface
import android.os.Bundle
import com.station.stationdownloader.databinding.DialogDoneTaskItemMenuBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.contract.GetContentActivityContract
import com.station.stationdownloader.ui.fragment.donetask.DownloadedTaskFragment
import com.station.stationdownloader.ui.fragment.donetask.UiAction
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DoneTaskItemMenuDialogFragment : BaseDialogFragment<DialogDoneTaskItemMenuBinding>() {

    private val te:GetContentActivityContract
    private val url: String by lazy {
        arguments?.getString(EXTRA_URL) ?: ""
    }

    private val vm by lazy {
        (requireParentFragment() as DownloadedTaskFragment).getViewModel()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.HideTaskMenu)
    }

    companion object {
        const val EXTRA_URL = "url"
        fun newInstance(url: String): DoneTaskItemMenuDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_URL, url)
            val fragment = DoneTaskItemMenuDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}