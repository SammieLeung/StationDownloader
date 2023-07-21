package com.station.stationdownloader.ui.fragment.downloading

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadingTaskFragment : BaseFragment<FragmentDownloadtaskBinding>(), DLogger {
    private val pVm by activityViewModels<MainViewModel>()
    private val vm by viewModels<DownloadTaskManageViewModel>()
    private val taskListAdapter by lazy {
        TaskListAdapter(vm.accept)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d("onViewCreated")
        vm.setTaskIdFlow(pVm.taskIdMap)
        mBinding.bindState(
            vm.taskItemList,
            vm.accept
        )
    }

    private fun FragmentDownloadtaskBinding.bindState(

        taskItemListFlow: Flow<List<TaskItem>>,
        accept: Any?
    ) {
        taskListView.adapter = taskListAdapter
//        taskListView.itemAnimator?.changeDuration = 0

        lifecycleScope.launch {
            taskItemListFlow.collect {
                taskListAdapter.fillData(it)
            }
        }

        lifecycleScope.launch {
            vm.status.collect {
                if (it is StatusState.Status)
                    taskListAdapter.updateProgress(it.taskItem)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        vm.accept(UiAction.UpdateProgress)
    }


    override fun onPause() {
        super.onPause()

    }

    companion object {
        fun newInstance(destination: Destination): DownloadingTaskFragment {
            val args = Bundle()
            val fragment = DownloadingTaskFragment()
            fragment.arguments = args
            args.putInt("destination", destination.ordinal)
            return fragment
        }
    }

    override fun DLogger.tag(): String {
        return DownloadingTaskFragment.javaClass.simpleName
    }
}
