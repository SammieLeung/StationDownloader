package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.FileType
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.databinding.DialogFragmentAddNewTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.utils.DLogger
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AddNewTaskDialogFragment : BaseDialogFragment<DialogFragmentAddNewTaskBinding>(), DLogger {
    private val vm: MainViewModel by activityViewModels<MainViewModel>()
    private val taskFileListAdapter: TreeNodeAdapter by lazy {
        TreeNodeAdapter()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.initRecyclerView()
        mBinding.initSpinner()
        mBinding.bindState(
            vm.newTaskState,
            vm.dialogAccept
        )
    }

    private fun DialogFragmentAddNewTaskBinding.initRecyclerView() {
        taskFileList.adapter = taskFileListAdapter
    }

    private fun DialogFragmentAddNewTaskBinding.initSpinner() {
        val adapter: StationSpinnerAdapter<CharSequence?> = StationSpinnerAdapter<CharSequence?>(
            requireContext(),
            arrayOf<String?>("默认下载", "Aria2下载")
        ) // 设置下拉菜单的样式
        adapter.setDropDownViewResource(com.station.stationtheme.R.layout.station_spinner_dropdown_item)
        // 将适配器绑定到spinner上
        engineSpinner.adapter = adapter
    }

    private fun DialogFragmentAddNewTaskBinding.bindState(
        newTaskState: Flow<NewTaskState>,
        dialogAccept: (DialogAction) -> Unit
    ) {
        cancelBtn.setOnClickListener {
            dismiss()
        }

        downloadBtn.setOnClickListener {
//            vm.accept(UiAction.StartDownloadTask(vm.))
        }


        val newTaskConfigFlow = newTaskState
            .filter {
            it is NewTaskState.PreparingData
        }.map {
            (it as NewTaskState.PreparingData)
        }.distinctUntilChanged()

        val fileFilterGroupFlow = newTaskState.filter { it is NewTaskState.PreparingData }.map {
            (it as NewTaskState.PreparingData).fileFilterGroup
        }.distinctUntilChanged()
        lifecycleScope.launch {
            newTaskState.collect{
                logger("newTaskState collect")
            }
        }
        lifecycleScope.launch {
            newTaskConfigFlow.collect {
                val task=it.task

                logger("newTaskConfigFlow collect ${task._fileTree.toString()}")
                taskName = task._name
                downloadPath = task._downloadPath
                engineSpinner.setSelection(task._downloadEngine.ordinal)
                taskFileListAdapter.fillData(task._fileTree as TreeNode.Directory)
            }
        }

        lifecycleScope.launch {
            fileFilterGroupFlow.collect {
                logger("fileFilterGroupFlow collect")
                unBindCheckBox(videoCBox, audioCBox, otherCBox, pictureCBox)

                videoCBox.isChecked = it.selectVideo
                audioCBox.isChecked = it.selectAudio
                otherCBox.isChecked = it.selectOther
                pictureCBox.isChecked = it.selectImage

                bindCheckBox(videoCBox, FileType.VIDEO, dialogAccept)
                bindCheckBox(audioCBox, FileType.AUDIO, dialogAccept)
                bindCheckBox(pictureCBox, FileType.IMG, dialogAccept)
                bindCheckBox(otherCBox, FileType.OTHER, dialogAccept)
            }
        }
    }

    private fun unBindCheckBox(vararg checkBoxes: CheckBox) {
        for (checkbox in checkBoxes) {
            checkbox.setOnCheckedChangeListener(null)
        }
    }

    private fun bindCheckBox(
        checkBox: CheckBox,
        fileType: FileType,
        dialogAccept: (DialogAction) -> Unit
    ) {
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            dialogAccept(DialogAction.CheckState(fileType, isChecked))
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        vm.dialogAccept(DialogAction.ResetTaskSettingDialogState)
    }

    override fun DLogger.tag(): String {
        return "AddNewTaskDialogFragment"
    }
}


