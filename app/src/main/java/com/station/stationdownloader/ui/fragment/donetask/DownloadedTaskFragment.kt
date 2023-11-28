package com.station.stationdownloader.ui.fragment.donetask

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.station.stationdownloader.R
import com.station.stationdownloader.databinding.FragmentDownloadtaskBinding
import com.station.stationdownloader.ui.base.BaseFragment
import com.station.stationdownloader.ui.fragment.donetask.menu.DoneTaskItemMenuDialogFragment
import com.station.stationdownloader.utils.DLogger
import com.station.stationkitkt.PackageTools
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

        emptyListHint = getString(R.string.done_task_list_empty)

        taskListView.adapter = taskListAdapter
        taskListView.itemAnimator = null

        lifecycleScope.launch {
            uiStateFlow.collect {
                when (it) {
                    is UiState.AddDoneTaskItemState -> {
                        isEmpty = false
                        taskListAdapter.addNewTask(it.doneItem)
                    }

                    is UiState.DeleteTaskResultState -> {
                        taskListAdapter.deleteTask(it.deleteItem)
                        isEmpty=taskListAdapter.itemCount==0
                    }

                    is UiState.FillDataList -> {
                        isEmpty = it.doneTaskItemList.isEmpty()
                        taskListAdapter.fillData(it.doneTaskItemList)
                    }
                    is UiState.OpenFileState->{
                        if (PackageTools.isAppInstalled(
                                requireContext(),
                                DoneTaskItemMenuDialogFragment.FIREFLY_FILE_MANAGER
                            )
                        ) {
                            openFileWithFirefly(it.uri)
                        } else {
                            openFile(it.uri)
                        }
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

    private fun openFileWithFirefly(uri: Uri) {
        val action = "${DoneTaskItemMenuDialogFragment.FIREFLY_FILE_MANAGER}.OPEN"
        startActivity(Intent(action).apply {
            putExtra("path", uri.path)
        })
    }

    private fun openFile(uri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            // Optionally, specify a URI for the file that should appear in the
            // system file picker when it loads.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent);
        } else {
            logger("处理没有文件管理器应用的情况")
        }
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