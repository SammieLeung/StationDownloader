package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.station.stationdownloader.FileType
import com.station.stationdownloader.R
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.databinding.DialogFragmentAddNewTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.contract.OpenDocumentTreeActivityResultContract
import com.station.stationdownloader.ui.contract.SelectFileActivityResultContract
import com.station.stationdownloader.ui.contract.SelectType
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.ui.viewmodel.UiAction
import com.station.stationdownloader.utils.DLogger
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


class AddNewTaskDialogFragment : BaseDialogFragment<DialogFragmentAddNewTaskBinding>(), DLogger {

    private val vm: MainViewModel by activityViewModels<MainViewModel>()
    private val stationPickerContract = SelectFileActivityResultContract(
        selectType = SelectType.SELECT_TYPE_FOLDER, showConfirmDialog = true
    )
    private val openDocumentTree = registerForActivityResult(
        OpenDocumentTreeActivityResultContract()
    ) {
        if (it != null) {
            //获取文件夹的永久访问权限（重启后仍然生效）
            requireContext().contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }


    private val openStationPicker = registerForActivityResult(stationPickerContract) {
        if (it != null) {
            val base64Id: String = it.pathSegments[1] //dir id
            val decodeData = String(Base64.decode(base64Id, Base64.DEFAULT))
            vm.dialogAccept(DialogAction.SetDownloadPath(decodeData))
        }

    }


    private val taskFileListAdapter: TreeNodeAdapter by lazy {
        TreeNodeAdapter { vm.dialogAccept(DialogAction.CalculateSizeInfo) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stationPickerContract.setPickerDialogTitle(getString(R.string.title_select_download_path))
        if (savedInstanceState == null) {
            mBinding.initRecyclerView()
            mBinding.initSpinner()
            mBinding.bindState(
                vm.newTaskState, vm.accept, vm.dialogAccept
            )
        }
    }

    private fun DialogFragmentAddNewTaskBinding.initRecyclerView() {
        taskFileList.adapter = taskFileListAdapter
        //动画时间设置为0，因为动画可能会导致项目闪烁
        taskFileList.itemAnimator?.changeDuration = 0
    }

    private fun DialogFragmentAddNewTaskBinding.initSpinner() {
        val adapter: StationSpinnerAdapter<CharSequence?> =
            StationSpinnerAdapter<CharSequence?>(
                requireContext(), arrayOf<String?>("默认下载", "Aria2下载")
            ) // 设置下拉菜单的样式
        // 将适配器绑定到spinner上
        engineSpinner.adapter = adapter
        engineSpinner.isEnabled=false
    }

    private fun DialogFragmentAddNewTaskBinding.bindState(
        newTaskState: Flow<NewTaskState>,
        accept: (UiAction) -> Unit,
        dialogAccept: (DialogAction) -> Unit
    ) {
        downloadPathView.isSelected = true
        cancelBtn.setOnClickListener {
            dismiss()
        }

        downloadBtn.setOnClickListener {
            accept(UiAction.StartDownloadTask)
        }

        filePickerBtn.setOnClickListener {
            openFilePicker()
        }

        collectFileTree(newTaskState)
        collectFileFilterGroup(newTaskState, dialogAccept)
        collectTaskSizeInfo(newTaskState)
        collectSuccess(newTaskState)
        collectDownloadPath(newTaskState)
        collectTaskName(newTaskState)
    }


    private fun collectFileTree(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter {
                it is NewTaskState.PreparingData
            }.map {
                (it as NewTaskState.PreparingData).task._fileTree
            }.distinctUntilChanged().collect {
                taskFileListAdapter.fillData(it as TreeNode.Directory)
            }
        }
    }

    private fun DialogFragmentAddNewTaskBinding.collectFileFilterGroup(
        newTaskState: Flow<NewTaskState>,
        dialogAccept: (DialogAction) -> Unit
    ) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingData }.map {
                (it as NewTaskState.PreparingData).fileFilterGroup
            }.distinctUntilChanged().collect {
                unBindCheckBox(videoCBox, audioCBox, otherCBox, pictureCBox)

                videoCBox.isChecked = it.selectVideo
                audioCBox.isChecked = it.selectAudio
                otherCBox.isChecked = it.selectOther
                pictureCBox.isChecked = it.selectImage

                bindCheckBox(videoCBox, FileType.VIDEO, dialogAccept)
                bindCheckBox(audioCBox, FileType.AUDIO, dialogAccept)
                bindCheckBox(pictureCBox, FileType.IMG, dialogAccept)
                bindCheckBox(otherCBox, FileType.OTHER, dialogAccept)
                taskFileListAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun DialogFragmentAddNewTaskBinding.collectTaskSizeInfo(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingData }.map {
                (it as NewTaskState.PreparingData).taskSizeInfo
            }.distinctUntilChanged().collect {
                taskSizeInfo = it.taskSizeInfo
                downloadSpace = it.downloadPathSizeInfo
            }
        }
    }

    private fun collectSuccess(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.Success }.collect {
                dismiss()
            }
        }
    }

    private fun DialogFragmentAddNewTaskBinding.collectTaskName(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingData }.map {
                (it as NewTaskState.PreparingData).task._name
            }.distinctUntilChanged().collect {
                taskName = it
            }
        }
    }

    private fun DialogFragmentAddNewTaskBinding.collectDownloadPath(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingData }.map {
                (it as NewTaskState.PreparingData).task._downloadPath
            }.distinctUntilChanged().collect {
                downloadPath = it
            }
        }
    }

    private fun openFilePicker() {
        openStationPicker.launch(null)
    }

    private fun unBindCheckBox(vararg checkBoxes: CheckBox) {
        for (checkbox in checkBoxes) {
            checkbox.setOnCheckedChangeListener(null)
        }
    }

    private fun bindCheckBox(
        checkBox: CheckBox, fileType: FileType, dialogAccept: (DialogAction) -> Unit
    ) {
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            dialogAccept(DialogAction.FilterGroupState(fileType, isChecked))
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





