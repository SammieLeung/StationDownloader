package com.station.stationdownloader.ui.fragment.downloading

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.R
import com.station.stationdownloader.databinding.TaskItemBinding
import com.station.stationkitkt.dp

class TaskViewHolder(val binding: TaskItemBinding, val accept: (UiAction) -> Unit) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(taskItem: TaskItem) {
        binding.taskItem = taskItem
        binding.root.setOnClickListener {
            if (taskItem.statuBtn == R.drawable.ic_stop) {
                accept(UiAction.StopTask(taskItem.url))
            } else {
                accept(UiAction.StartTask(taskItem.url))
            }

        }
        binding.root.setOnLongClickListener {
            accept(UiAction.ShowTaskMenu(taskItem.url))
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
        fun create(parent: ViewGroup, accept: (UiAction) -> Unit): TaskViewHolder {
            return TaskViewHolder(
                TaskItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                ), accept
            )
        }
    }
}