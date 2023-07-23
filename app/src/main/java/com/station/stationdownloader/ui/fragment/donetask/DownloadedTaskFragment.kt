package com.station.stationdownloader.ui.fragment.donetask

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadedTaskFragment : BaseFragment<FragmentDownloadtaskBinding>(),DLogger {

    private val vm by viewModels<DownloadedTaskViewModel>()
    private val taskListAdapter by lazy {
        DoneTaskListAdapter(vm.accept)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.taskList
        )
    }
    override fun onResume() {
        super.onResume()
        logger("onResume")
        vm.accept(UiAction.getTaskList)
    }

    fun FragmentDownloadtaskBinding.bindState(taskItemListFlow: SharedFlow<List<DoneTaskItem>>) {
        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator=null

        lifecycleScope.launch {
            taskItemListFlow.collect {
                logger("fillData= ${it}")
                taskListAdapter.fillData(it)
            }
        }
    }

    override fun DLogger.tag(): String {
      return DownloadedTaskFragment::class.java.simpleName
    }
}