package com.station.stationdownloader.ui.fragment.downloading.menu

import android.content.DialogInterface
import android.os.Bundle
import com.station.stationdownloader.databinding.DialogTaskItemMenuBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.fragment.downloading.DownloadingTaskFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskItemMenuDialogFragment : BaseDialogFragment<DialogTaskItemMenuBinding>() {
    private val url: String by lazy {
        arguments?.getString(EXTRA_URL) ?: ""
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(requireParentFragment() is DownloadingTaskFragment){
            (requireParentFragment() as DownloadingTaskFragment).hideTaskItemMenu()
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        fun newInstance(url: String): TaskItemMenuDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_URL, url)
            val fragment = TaskItemMenuDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}