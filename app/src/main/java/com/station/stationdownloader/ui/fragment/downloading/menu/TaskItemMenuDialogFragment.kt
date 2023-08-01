package com.station.stationdownloader.ui.fragment.downloading.menu

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.DialogTaskItemMenuBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.fragment.downloading.DownloadingTaskFragment
import com.station.stationdownloader.ui.fragment.downloading.DownloadingTaskViewModel
import com.station.stationdownloader.ui.fragment.downloading.MenuDialogUiState
import com.station.stationdownloader.ui.fragment.downloading.UiAction
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
        mBinding.bindState(vm.accept, vm.menuState)

    }

    private fun DialogTaskItemMenuBinding.bindState(
        accept: (UiAction) -> Unit,
        menuState: StateFlow<MenuDialogUiState>
    ) {
        deleteTaskBtn.setOnClickListener {
            showConfirmDeleteDialog()
        }

        lifecycleScope.launch {
            menuState.collect {
                isTaskRunning = it.isTaskRunning
                if (isTaskRunning) {
                    stopTaskBtn.setOnClickListener {
                        accept(UiAction.StopTask(url))
                        dismiss()
                    }
                } else {
                    startTaskBtn.setOnClickListener {
                        accept(UiAction.StartTask(url))
                        dismiss()
                    }
                }

                if (it.isDelete) {
                    dismiss()
                }
                if (!it.isShow) {
                    dismiss()
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.accept(UiAction.ShowTaskMenu(url, false))
        vm.accept(UiAction.InitUiState)
    }

    private fun showConfirmDeleteDialog() {
        parentFragmentManager.beginTransaction()
            .add(ConfirmDeleteDialogFragment.newInstance(url), "confirm_delete")
            .hide(this)
            .addToBackStack("confirm_delete")
            .commit()
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