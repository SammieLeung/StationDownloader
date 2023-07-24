package com.station.stationdownloader.ui.fragment.downloading.menu

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.station.stationdownloader.databinding.DialogTaskItemMenuBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.fragment.downloading.DownloadingTaskFragment
import com.station.stationdownloader.ui.fragment.downloading.UiAction
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TaskItemMenuDialogFragment : BaseDialogFragment<DialogTaskItemMenuBinding>() {
    private val url: String by lazy {
        arguments?.getString(EXTRA_URL) ?: ""
    }
    private val isTaskRunning: Boolean by lazy {
        arguments?.getBoolean(EXTRA_IS_TASK_RUNNING) ?: false
    }

    private val vm by lazy {
        (requireParentFragment() as DownloadingTaskFragment).getViewModel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.isTaskRunning=isTaskRunning
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.HideTaskMenu)
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_IS_TASK_RUNNING = "is_task_running"
        fun newInstance(url: String, isTaskRunning: Boolean): TaskItemMenuDialogFragment {
            val args = Bundle()
            args.putString(EXTRA_URL, url)
            args.putBoolean(EXTRA_IS_TASK_RUNNING, isTaskRunning)
            val fragment = TaskItemMenuDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}