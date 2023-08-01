package com.station.stationdownloader.ui.fragment.donetask

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.fragment.downloading.menu.DoneTaskItemMenuDialogFragment
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadedTaskFragment : BaseFragment<FragmentDownloadtaskBinding>(), DLogger {

    private val vm by viewModels<DownloadedTaskViewModel>()
    private val taskListAdapter by lazy {
        DoneTaskListAdapter(vm.accept)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.uiState,
            vm.menuDialogUiState
        )
    }

    override fun onResume() {
        super.onResume()
        vm.accept(UiAction.GetTaskList)
        bindReceiver()
    }

    override fun onPause() {
        super.onPause()
        unbindReceiver()
    }

    override fun DLogger.tag(): String {
        return DownloadedTaskFragment::class.java.simpleName
    }


    fun FragmentDownloadtaskBinding.bindState(
        uiStateFlow: StateFlow<UiState>,
        menuDialogUiState: StateFlow<MenuDialogUiState>
    ) {
        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator = null

        lifecycleScope.launch {
            uiStateFlow.collect {
                when (it) {
                    is UiState.AddDoneTaskItemState -> {
                        taskListAdapter.addNewTask(it.doneItem)
                    }

                    is UiState.DeleteTaskResultState -> {
                        taskListAdapter.deleteTask(it.deleteItem)
                    }

                    is UiState.FillDataList -> {
                        taskListAdapter.fillData(it.doneTaskItemList)
                    }
                    else -> {}
                }
            }
        }

        lifecycleScope.launch {
            menuDialogUiState.collect {
                if (it.isShow) {
                    val dialog = DoneTaskItemMenuDialogFragment.newInstance(it.url)
                    dialog.show(childFragmentManager, "DoneTaskItemMenuDialogFragment")
                }
            }
        }
    }

    fun getViewModel(): DownloadedTaskViewModel {
        return vm
    }


    private fun bindReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            vm.broadcastReceiver,
            vm.intentFilter
        )
    }

    private fun unbindReceiver() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(
            vm.broadcastReceiver
        )
    }

}