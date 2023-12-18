package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.DialogFragmentMultiTaskDetailBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.ui.viewmodel.UiAction
import com.station.stationdownloader.utils.DLogger
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MultiTaskDetailDialogFragment : BaseDialogFragment<DialogFragmentMultiTaskDetailBinding>(),
    DLogger {
    private val vm: MainViewModel by activityViewModels()
    private var isConfirm: Boolean = false
    override fun DLogger.tag(): String {
        return "MultiTaskDetailDialogFragment"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            mBinding.initRecyclerView(vm.newTaskState)
            mBinding.bindState(vm.newTaskState, vm.accept, vm.dialogAccept)
        }
    }

    fun DialogFragmentMultiTaskDetailBinding.bindState(
        newTaskState: StateFlow<NewTaskState>,
        accept: (UiAction) -> Unit,
        dialogAccept: (DialogAction) -> Unit
    ) {

        downloadBtn.setOnClickListener {
            isConfirm = true
            accept(UiAction.StartDownloadTask)
        }

        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData).multiNewTaskConfig
            }.distinctUntilChanged().collect {
                failedLinksView.visibility=if(it.failedLinks.isNotEmpty()) View.VISIBLE else View.GONE
                successLinksView.visibility=if(it.taskConfigs.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.Success }.collect {
                dismiss()
            }
        }
    }

    fun DialogFragmentMultiTaskDetailBinding.initRecyclerView(newTaskState: StateFlow<NewTaskState>){
        val adapter=SingleTaskInfoAdapter()
        successLinksRV.adapter=adapter
        val failedLinkAdapter=FailedLinkAdapter()
        failedLinksRV.adapter=failedLinkAdapter
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData).multiNewTaskConfig
            }.distinctUntilChanged().collect {
                adapter.attachData(it)
                failedLinkAdapter.attachData(it.failedLinks)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if(isConfirm) {
            vm.dialogAccept(DialogAction.ReinitializeAllDialogAction)
        }else{
            vm.dialogAccept(DialogAction.BackToAddMultiTask)
        }
    }
}