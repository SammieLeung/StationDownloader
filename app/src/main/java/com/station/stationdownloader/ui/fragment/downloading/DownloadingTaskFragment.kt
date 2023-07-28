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
            vm.uiState,
            vm.accept,
        )
    }

    override fun onResume() {
        super.onResume()
        logger("onResume")
        vm.accept(UiAction.GetTaskList)
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
        uiStateFlow: StateFlow<UiState>,
        accept: (UiAction) -> Unit,
    ) {
        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator = null
        lifecycleScope.launch {
            newTaskState.collect {
                if (it is NewTaskState.Success) {
                    accept(UiAction.GetTaskList)
                }
            }
        }

        lifecycleScope.launch {
            uiStateFlow.collect {
                when (it) {
                    is UiState.Init -> {}
                    is UiState.FillTaskList -> {
                        taskListAdapter.fillData(it.taskList)
                    }
                    is UiState.UpdateProgress -> {
                        taskListAdapter.updateProgress(it.taskItem)
                    }

                    UiState.HideMenu -> {
                    }
                    is UiState.ShowMenu -> {
                        val dialog = TaskItemMenuDialogFragment.newInstance(it.url, it.isTaskRunning)
                        dialog.show(childFragmentManager, "TaskItemMenuDialogFragment")
                    }
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

