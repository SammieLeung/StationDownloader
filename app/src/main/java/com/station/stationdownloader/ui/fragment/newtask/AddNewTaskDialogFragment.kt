package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.databinding.DialogFragmentAddNewTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.TaskSettingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddNewTaskDialogFragment : BaseDialogFragment<DialogFragmentAddNewTaskBinding>() {
    val vm: MainViewModel by activityViewModels<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.taskSettingState,
            vm.dialogAccept
        )
    }

    fun DialogFragmentAddNewTaskBinding.bindState(
        configState: Flow<TaskSettingState>,
        dialogAccept: (DialogAction) -> Unit
    ) {
        mBinding.cancelBtn.setOnClickListener {
            dismiss()
        }

        mBinding.downloadBtn.setOnClickListener {
        }

        bindCheckBox(mBinding.videoCBox, MainViewModel.VIDEO_FILE, dialogAccept)
        bindCheckBox(mBinding.audioCBox, MainViewModel.AUDIO_FILE, dialogAccept)
        bindCheckBox(mBinding.pictureCBox, MainViewModel.PICTURE_FILE, dialogAccept)
        bindCheckBox(mBinding.otherCBox, MainViewModel.OTHER_FILE, dialogAccept)


        lifecycleScope.launch {
            configState.collect {
                if (it is TaskSettingState.PreparingData) {
                    val adapter = FileStateAdapter()
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


