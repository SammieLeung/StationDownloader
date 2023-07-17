package com.station.stationdownloader.ui.fragment.newtask

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.CheckBox
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
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
import com.station.stationdownloader.utils.DLogger
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File


class AddNewTaskDialogFragment : BaseDialogFragment<DialogFragmentAddNewTaskBinding>(), DLogger {

    private val vm: MainViewModel by activityViewModels<MainViewModel>()
    private val stationPickerContract = SelectFileActivityResultContract(
        selectType = SelectType.SELECT_TYPE_FOLDER, showConfirmDialog = true
    )
    private val openDocumentTree = registerForActivityResult(
        OpenDocumentTreeActivityResultContract()
    ) {
        if (it != null && context != null) {
            //获取文件夹的永久访问权限（重启后仍然生效）
            context?.contentResolver?.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            logger(it)
            logger(DocumentsContract.getTreeDocumentId(it))
            val rootUri = DocumentsContract.buildTreeDocumentUri("com.android.externalstorage.documents","primary:Station")
            val rootDocumentFile = DocumentFile.fromSingleUri(requireContext(), rootUri)
            logger(rootDocumentFile?.uri)
        }
    }


    private val openStationPicker = registerForActivityResult(stationPickerContract) {
        if (it != null) {
            val base64Id: String = it.pathSegments[1] //dir id
            val decodeData = String(Base64.decode(base64Id, Base64.DEFAULT))
            vm.dialogAccept(DialogAction.DownloadPath(decodeData))
        }

    }


    private val taskFileListAdapter: TreeNodeAdapter by lazy {
        TreeNodeAdapter()
    }

    fun convertDocumentFileToFile(context: Context, documentFile: DocumentFile?): File? {
        if(documentFile==null)
            return null
        val documentId = DocumentsContract.getDocumentId(documentFile.uri)
        val split = documentId.split(":".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val type = split[0]
        val uri = MediaStore.Files.getContentUri("external")
        val selection = MediaStore.Files.FileColumns._ID + "=?"
        val selectionArgs = arrayOf(split[1])
        val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
        val sortOrder: String? = null
        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                .use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val columnIndex =
                            cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                        val path = cursor.getString(columnIndex)
                        return File(path)
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        stationPickerContract.setPickerDialogTitle(getString(R.string.title_select_download_path))
        if (savedInstanceState == null) {
            mBinding.initRecyclerView()
            mBinding.initSpinner()
            mBinding.bindState(
                vm.newTaskState, vm.dialogAccept
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
    }

    private fun DialogFragmentAddNewTaskBinding.bindState(
        newTaskState: Flow<NewTaskState>, dialogAccept: (DialogAction) -> Unit
    ) {
        cancelBtn.setOnClickListener {
            dismiss()
        }

        downloadBtn.setOnClickListener {}

        filePickerBtn.setOnClickListener {
            openFilePicker()
        }


        val newTaskConfigFlow = newTaskState.filter {
            it is NewTaskState.PreparingData
        }.map {
            (it as NewTaskState.PreparingData)
        }.distinctUntilChanged()

        val fileFilterGroupFlow = newTaskState.filter { it is NewTaskState.PreparingData }.map {
            (it as NewTaskState.PreparingData).fileFilterGroup
        }.distinctUntilChanged()
        lifecycleScope.launch {
            newTaskConfigFlow.collect {
                val task = it.task
                taskName = task._name
                downloadPath = task._downloadPath
                engineSpinner.setSelection(task._downloadEngine.ordinal)
                taskFileListAdapter.fillData(task._fileTree as TreeNode.Directory)
            }
        }

        lifecycleScope.launch {
            fileFilterGroupFlow.collect {
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

    private fun openFilePicker() {
        openDocumentTree.launch(null)
//        openStationPicker.launch(null)
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





