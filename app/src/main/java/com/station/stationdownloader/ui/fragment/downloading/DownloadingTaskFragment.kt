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
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.fragment.downloading.menu.DoneTaskItemMenuDialogFragment
import com.station.stationdownloader.ui.fragment.downloading.menu.TaskItemMenuDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
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
            vm.taskItemList,
            vm.statusState,
            vm.taskMenuState,
            vm.accept,
        )
    }

    override fun onResume() {
        super.onResume()
        logger("onResume")
        vm.accept(UiAction.getTaskList)
        bindService()
    }


    override fun onPause() {
        super.onPause()
        unbindService()
    }

    override fun DLogger.tag(): String {
        return DownloadingTaskFragment::class.java.simpleName
    }


    fun getViewModel(): DownloadingTaskViewModel {
        return vm
    }

    private fun FragmentDownloadtaskBinding.bindState(
        newTaskState: StateFlow<NewTaskState>,
        taskItemListFlow: StateFlow<List<TaskItem>>,
        status: StateFlow<StatusState>,
        menuState: StateFlow<TaskMenuState>,
        accept: (UiAction) -> Unit,
    ) {
        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator = null
        lifecycleScope.launch {
            newTaskState.collect {
                if (it is NewTaskState.SUCCESS) {
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
                if (it is StatusState.Status) {
                    logger("newStatus= ${it.taskItem.sizeInfo} ${it.taskItem.speed}")
                    taskListAdapter.updateProgress(it.taskItem)
                }
            }
        }

        lifecycleScope.launch {
            menuState.collect {
                if (it is TaskMenuState.Show) {
                    val dialog = TaskItemMenuDialogFragment.newInstance(it.url,it.isTaskRunning)
                    dialog.show(childFragmentManager, "TaskItemMenuDialogFragment")
                }
            }
        }

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

}

