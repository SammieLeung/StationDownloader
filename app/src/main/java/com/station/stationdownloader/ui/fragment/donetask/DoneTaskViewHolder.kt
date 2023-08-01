package com.station.stationdownloader.ui.fragment.donetask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.databinding.DoneTaskItemBinding
import com.station.stationkitkt.dp

class DoneTaskViewHolder(val binding: DoneTaskItemBinding, val accept: (UiAction) -> Unit) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(taskItem: DoneTaskItem) {
        binding.taskItem = taskItem
        binding.root.setOnClickListener {


        }
        binding.root.setOnLongClickListener{
            accept(UiAction.ShowTaskMenu(taskItem.url,true))
            true
        }
        if (this.absoluteAdapterPosition == 0) {
            binding.root.updateLayoutParams<RecyclerView.LayoutParams> {
                this.topMargin = 50.dp
            }
        } else {
            binding.root.updateLayoutParams<RecyclerView.LayoutParams> {
                this.topMargin = 0
            }
        }
    }

    companion object {
        fun create(parent: ViewGroup, accept: (UiAction) -> Unit): DoneTaskViewHolder {
            return DoneTaskViewHolder(
                DoneTaskItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), accept
            )
        }
    }
}