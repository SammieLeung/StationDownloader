package com.station.stationdownloader.ui.fragment.donetask

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.fragment.downloading.menu.DoneTaskItemMenuDialogFragment
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
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
            vm.taskList,
            vm.taskMenuState
        )
    }

    override fun onResume() {
        super.onResume()
        logger("onResume")
        vm.accept(UiAction.GetTaskList)
    }

    override fun DLogger.tag(): String {
        return DownloadedTaskFragment::class.java.simpleName
    }

    fun FragmentDownloadtaskBinding.bindState(
        taskItemListFlow: SharedFlow<List<DoneTaskItem>>,
        taskMenuState: StateFlow<TaskMenuState>
    ) {
        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator = null

        lifecycleScope.launch {
            taskItemListFlow.collect {
                logger("fillData= ${it}")
                taskListAdapter.fillData(it)
            }
        }

        lifecycleScope.launch {
            taskMenuState.collect {
                if (it is TaskMenuState.Show) {
                    val dialog = DoneTaskItemMenuDialogFragment.newInstance(it.url)
                    dialog.show(childFragmentManager, "DoneTaskItemMenuDialogFragment")
                }
            }
        }
    }

    fun getViewModel(): DownloadedTaskViewModel {
        return vm
    }

}