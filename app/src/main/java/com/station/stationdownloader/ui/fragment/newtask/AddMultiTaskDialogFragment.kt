package com.station.stationdownloader.ui.fragment.newtask

import android.content.DialogInterface
import android.os.Bundle
import android.text.Html
import android.util.Base64
import android.view.View
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.FileType
import com.station.stationdownloader.R
import com.station.stationdownloader.StationDownloaderApp
import com.station.stationdownloader.databinding.DialogFragmentAddMultiTaskBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.contract.OpenFileManagerV1Contract
import com.station.stationdownloader.ui.contract.OpenFileManagerV2Contract
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.ui.viewmodel.UiAction
import com.station.stationdownloader.utils.DLogger
import com.station.stationtheme.spinner.StationSpinnerAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AddMultiTaskDialogFragment : BaseDialogFragment<DialogFragmentAddMultiTaskBinding>(),
    DLogger {
    private val app by lazy {
        requireActivity().application as StationDownloaderApp
    }
    private val vm: MainViewModel by activityViewModels()
    private var isShowDetail: Boolean = false

    private val openFolderV1 = registerForActivityResult(OpenFileManagerV1Contract()) {
        if (it != null) {
            val base64Id: String = it.pathSegments[1] //dir id
            val decodeData = String(Base64.decode(base64Id, Base64.DEFAULT))
            vm.dialogAccept(DialogAction.ChangeDownloadPath(decodeData))
        }
    }

    private val openFolderV2 = registerForActivityResult(OpenFileManagerV2Contract()) {
        it?.let { intent ->
            val dataType = intent.getIntExtra("data_type", -1)
            if (dataType == 2) {
                val uri = intent.getStringExtra("path") ?: return@registerForActivityResult
                vm.dialogAccept(DialogAction.ChangeDownloadPath(uri))
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            mBinding.initSpinner(vm.newTaskState, vm.dialogAccept)
            mBinding.bindState(
                vm.newTaskState, vm.accept, vm.dialogAccept
            )
        }
    }

    private fun DialogFragmentAddMultiTaskBinding.initSpinner(newTaskState: StateFlow<NewTaskState>, dialogAccept: (DialogAction) -> Unit) {
        val adapter: StationSpinnerAdapter<CharSequence?> =
            StationSpinnerAdapter<CharSequence?>(
                requireContext(),
                arrayOf<String?>(
                    getString(R.string.download_with_xl),
                    getString(R.string.download_with_aria2)
                )
            ) // 设置下拉菜单的样式
        // 将适配器绑定到spinner上
        engineSpinner.adapter = adapter
        engineSpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                }

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    when (position) {
                        0 -> dialogAccept(DialogAction.ChangeDownloadEngine(DownloadEngine.XL))
                        1 -> dialogAccept(DialogAction.ChangeDownloadEngine(DownloadEngine.ARIA2))
                    }
                }

            }
        collectDownloadEngine(newTaskState)
    }

    private fun DialogFragmentAddMultiTaskBinding.bindState(
        newTaskState: Flow<NewTaskState>,
        accept: (UiAction) -> Unit,
        dialogAccept: (DialogAction) -> Unit
    ) {
        downloadPathView.isSelected = true

        downloadBtn.setOnClickListener {
            accept(UiAction.StartDownloadTask)
        }

        filePickerBtn.setOnClickListener {
            openFilePicker()
        }

        collectInitializationProgress(newTaskState, dialogAccept)
        collectFileFilterGroup(newTaskState, dialogAccept)
        collectTaskSizeInfo(newTaskState)
        collectSuccess(newTaskState)
        collectDownloadPath(newTaskState)
    }


    private fun DialogFragmentAddMultiTaskBinding.collectInitializationProgress(
        newTaskState: Flow<NewTaskState>,
        dialogAccept: (DialogAction) -> Unit
    ) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData)
            }.distinctUntilChanged().collect {
                downloadBtn.isEnabled = it.isPrepared
                statusTextView.visibility = if (it.isPrepared) View.GONE else View.VISIBLE
                showFailedDetailView.visibility = if (it.isPrepared) View.VISIBLE else View.GONE
                if (it.isPrepared) {
                    initializeResultView.setOnClickListener {
                        isShowDetail = true
                        dialogAccept(DialogAction.ShowMultiTaskDetail)
                    }
                } else {
                    initializeResultView.isClickable = false
                }
                initializingUrl = getString(R.string.initailizing_url, it.initializingUrl)
                initializeResult = getString(
                    R.string.initailizing_result,
                    (it.multiNewTaskConfig.taskConfigs.size + it.multiNewTaskConfig.failedLinks.size).toString(),
                    it.multiNewTaskConfig.linkCount.toString()
                )
                initSucceeded = getString(
                    R.string.initailized_success,
                    it.multiNewTaskConfig.taskConfigs.size.toString()
                )
                initFailed = getString(
                    R.string.initailized_failed,
                    it.multiNewTaskConfig.failedLinks.size.toString()
                )
            }
        }
    }


    private fun DialogFragmentAddMultiTaskBinding.collectFileFilterGroup(
        newTaskState: Flow<NewTaskState>,
        dialogAccept: (DialogAction) -> Unit
    ) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData).fileFilterGroup
            }.distinctUntilChanged().collect {
                unBindCheckBox(allCBox, videoCBox, audioCBox, otherCBox, pictureCBox)

                allCBox.isChecked = it.selectAll
                videoCBox.isChecked = it.selectVideo
                audioCBox.isChecked = it.selectAudio
                otherCBox.isChecked = it.selectOther
                pictureCBox.isChecked = it.selectImage

                bindCheckBox(allCBox, FileType.ALL, dialogAccept)
                bindCheckBox(videoCBox, FileType.VIDEO, dialogAccept)
                bindCheckBox(audioCBox, FileType.AUDIO, dialogAccept)
                bindCheckBox(pictureCBox, FileType.IMG, dialogAccept)
                bindCheckBox(otherCBox, FileType.OTHER, dialogAccept)
            }
        }
    }

    private fun DialogFragmentAddMultiTaskBinding.collectTaskSizeInfo(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData).taskSizeInfo
            }.distinctUntilChanged().collect {
                taskSizeInfo = it.taskSizeInfo
                downloadSpaceView.setText(
                    Html.fromHtml(
                        it.downloadPathSizeInfo,
                        Html.FROM_HTML_OPTION_USE_CSS_COLORS
                    )
                )
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

    private fun DialogFragmentAddMultiTaskBinding.collectDownloadPath(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData).multiNewTaskConfig.downloadPath
            }.distinctUntilChanged().collect {
                downloadPath = it
            }
        }
    }

    private fun DialogFragmentAddMultiTaskBinding.collectDownloadEngine(newTaskState: Flow<NewTaskState>) {
        lifecycleScope.launch {
            newTaskState.filter { it is NewTaskState.PreparingMultiData }.map {
                (it as NewTaskState.PreparingMultiData).multiNewTaskConfig.engine
            }.distinctUntilChanged().collect {
                when (it) {
                    DownloadEngine.XL -> engineSpinner.setSelection(0)
                    DownloadEngine.ARIA2 -> engineSpinner.setSelection(1)
                    else -> engineSpinner.setSelection(0)
                }
            }
        }
    }


    private fun openFilePicker() {
        if (app.useV2FileManager) {
            openFileManagerV2()
        } else {
            openFileManagerV1()
        }
    }


    private fun openFileManagerV1() {
        openFolderV1.launch(Bundle().apply {
            putInt(
                OpenFileManagerV1Contract.EXTRA_SELECT_TYPE,
                OpenFileManagerV1Contract.SelectType.SELECT_TYPE_FOLDER.ordinal
            )
            putBoolean(OpenFileManagerV1Contract.EXTRA_SUPPORT_NET, false)
            putString(
                OpenFileManagerV1Contract.EXTRA_TITLE,
                getString(R.string.title_select_download_path)
            )
            putBoolean(OpenFileManagerV1Contract.EXTRA_CONFIRM_DIALOG, true)
        })
    }

    private fun openFileManagerV2() {
        openFolderV2.launch(Bundle().apply {
            putString(OpenFileManagerV2Contract.EXTRA_MIME_TYPE, "folder/*")
        })
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
        if (!isShowDetail)
            vm.dialogAccept(DialogAction.ReinitializeAllDialogAction)
    }

    override fun DLogger.tag(): String {
        return "AddMultiTaskDialogFragment"
    }
}