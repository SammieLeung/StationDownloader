package com.station.stationdownloader.ui.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.databinding.DialogFragmentTaskConfigBinding
import com.station.stationdownloader.databinding.FileItemBinding
import com.station.stationdownloader.ui.base.BaseDialogFragment
import com.station.stationdownloader.ui.viewmodel.DialogAction
import com.station.stationdownloader.ui.viewmodel.FileState
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.TaskSettingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AddTaskDialogFragment : BaseDialogFragment<DialogFragmentTaskConfigBinding>() {
    val vm: MainViewModel by activityViewModels<MainViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.bindState(
            vm.taskSettingState,
            vm.dialogAccept
        )
    }

    fun DialogFragmentTaskConfigBinding.bindState(
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


class FileStateAdapter :
    PagingDataAdapter<FileState, FileStateAdapter.FileViewHolder>(FILE_STATE_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder.create(parent, viewType)
    }


    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileStateItem = getItem(position)
        if (fileStateItem != null) {
            holder.bind(fileStateItem)
        }
    }

    companion object {
        private val FILE_STATE_COMPARATOR = object : DiffUtil.ItemCallback<FileState>() {
            override fun areItemsTheSame(oldItem: FileState, newItem: FileState): Boolean =
                oldItem.fileIndex == newItem.fileIndex

            override fun areContentsTheSame(oldItem: FileState, newItem: FileState): Boolean =
                oldItem == newItem
        }
    }


    class FileViewHolder(val binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fileStateItem: FileState) {
            binding.fileName.setText(fileStateItem.fileName)
            binding.checkbox.isChecked = fileStateItem.isChecked
        }


        companion object {
            fun create(parent: ViewGroup, viewType: Int): FileViewHolder {
                val binding = FileItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FileViewHolder(binding)
            }
        }
    }

}