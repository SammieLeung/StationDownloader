package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Spinner
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.R
import com.station.stationdownloader.databinding.DialogFragmentAddNewTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddNewTaskDialogFragment : BaseDialogFragment<DialogFragmentAddNewTaskBinding>() {
    val vm: MainViewModel by activityViewModels<MainViewModel>()
    val taskFileListAdapter: TreeNodeAdapter = TreeNodeAdapter()
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
        }

        bindCheckBox(videoCBox, MainViewModel.VIDEO_FILE, dialogAccept)
        bindCheckBox(audioCBox, MainViewModel.AUDIO_FILE, dialogAccept)
        bindCheckBox(pictureCBox, MainViewModel.PICTURE_FILE, dialogAccept)
        bindCheckBox(otherCBox, MainViewModel.OTHER_FILE, dialogAccept)


        lifecycleScope.launch {
            newTaskState.collect {
                if (it is NewTaskState.PreparingData) {
                    taskName = it.name
                    downloadPath = it.downloadPath
                    engineSpinner.setSelection(it.engine.ordinal)
                    taskFileListAdapter.fillData(it.fileTree)
                }
            }
        }
    }


    private fun bindCheckBox(
        checkBox: CheckBox,
        fileType: Int,
        dialogAccept: (DialogAction) -> Unit
    ) {
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
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
}


