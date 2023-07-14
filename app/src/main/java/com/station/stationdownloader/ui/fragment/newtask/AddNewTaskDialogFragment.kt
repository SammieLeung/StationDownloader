package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
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
    val vm: MainViewModel by activityViewModels<MainViewModel>()
    val taskFileListAdapter: TreeNodeAdapter by lazy {
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

    fun DialogFragmentAddNewTaskBinding.bindState(
        newTaskState: Flow<NewTaskState>,
        dialogAccept: (DialogAction) -> Unit
    ) {
        cancelBtn.setOnClickListener {
            dismiss()
        }

        downloadBtn.setOnClickListener {
//            vm.accept(UiAction.StartDownloadTask(vm.))
        }




        lifecycleScope.launch {
            newTaskState.collect {
                if (it is NewTaskState.PreparingData) {
                    val data = it.task
                    taskName = data._name
                    downloadPath = data._downloadPath
                    engineSpinner.setSelection(data._downloadEngine.ordinal)
                    taskFileListAdapter.fillData(data._fileTree as TreeNode.Root)
                    videoCBox.isChecked = it.selectVideo
                    audioCBox.isChecked = it.selectAudio
                    otherCBox.isChecked = it.selectOther
                    pictureCBox.isChecked = it.selectImage

                    bindCheckBox(videoCBox, MainViewModel.VIDEO_FILE, dialogAccept)
                    bindCheckBox(audioCBox, MainViewModel.AUDIO_FILE, dialogAccept)
                    bindCheckBox(pictureCBox, MainViewModel.PICTURE_FILE, dialogAccept)
                    bindCheckBox(otherCBox, MainViewModel.OTHER_FILE, dialogAccept)
                }
            }
        }

        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingData }.map { (it as NewTaskState.PreparingData).selectVideo }.distinctUntilChanged().collect{
                logger("selectVideo $it")
                vm.updateVideo()
            }
        }

        lifecycleScope.launch {
        }
    }


    private fun bindCheckBox(
        checkBox: CheckBox,
        fileType: Int,
        dialogAccept: (DialogAction) -> Unit
    ) {
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            logger("p0 ${buttonView?.isChecked}")
            logger("p1 $isChecked")
            if (isChecked) {
                dialogAccept(DialogAction.CheckAll(fileType))
            } else {
                dialogAccept(DialogAction.UnCheckAll(fileType))
            }
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


