package com.station.stationdownloader.ui.fragment.downloading

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DownloadingTaskFragment : BaseFragment<FragmentDownloadtaskBinding>(), DLogger {
    private val pVm by activityViewModels<MainViewModel>()
    private val vm by viewModels<DownloadingTaskViewModel>()
    private val taskListAdapter by lazy {
        TaskListAdapter(vm.accept)
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Logger.w("onServiceConnected")
            service as TaskService.TaskBinder
            vm.setTaskStatus(service.getService().getStatusFlow())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Logger.e("onServiceDisconnected")
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Logger.d("onViewCreated")
        mBinding.bindState(
            pVm.newTaskState,
            vm.taskList,
            vm.accept,
            vm.statusState,
        )
    }

    private fun FragmentDownloadtaskBinding.bindState(
        newTaskState: StateFlow<NewTaskState>,
        taskItemListFlow: Flow<List<TaskItem>>,
        accept: (UiAction) -> Unit,
        status: StateFlow<StatusState>
    ) {
        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator=null
        lifecycleScope.launch {
            newTaskState.collect{
                if(it is NewTaskState.SUCCESS){
                    accept(UiAction.getTaskList)
                }
            }
        }

        lifecycleScope.launch {
            taskItemListFlow.collect {
                logger("fillData= ${it}")
                taskListAdapter.fillData(it)
            }
        }

        lifecycleScope.launch {
            status.collect {
                if (it is StatusState.Status){
                    logger("newStatus= ${it.taskItem.sizeInfo} ${it.taskItem.speed}")
                    taskListAdapter.updateProgress(it.taskItem)

                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        logger("onResume")
        vm.accept(UiAction.getTaskList)
        vm.accept(UiAction.UpdateProgress)

        bindService()
    }


    override fun onPause() {
        super.onPause()
        unbindService()
    }


    private fun bindService() {
        requireContext().bindService(
            Intent(requireContext(), TaskService::class.java), serviceConnection,
            Service.BIND_AUTO_CREATE
        )
    }

    private fun unbindService() {
        requireContext().unbindService(serviceConnection)
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
        return DownloadingTaskFragment::class.java.simpleName
    }
}
