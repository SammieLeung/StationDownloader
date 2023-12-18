package com.station.stationdownloader.ui.fragment.newtask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.databinding.SingleTaskInfoItemBinding
import com.station.stationdownloader.utils.DLogger

class SingleTaskInfoViewHolder(
    val binding: SingleTaskInfoItemBinding,
) : RecyclerView.ViewHolder(binding.root),
    DLogger {

    fun bind(newTaskConfigModel: NewTaskConfigModel) {
        binding.taskName = newTaskConfigModel._name
        binding.selectedFileCount = (newTaskConfigModel._fileTree as TreeNode.Directory)
            .checkedFileCount.toString()

        binding.selectedFileSize = newTaskConfigModel._fileTree
            .totalCheckedFileSize.toHumanReading()
        binding.root.isClickable=true
    }

    override fun DLogger.tag(): String {
        return "SingleTaskInfoView"
    }

    companion object {
        fun create(
            parent: ViewGroup,
        ): SingleTaskInfoViewHolder {
            val binding = SingleTaskInfoItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return SingleTaskInfoViewHolder(binding)
        }
    }
}