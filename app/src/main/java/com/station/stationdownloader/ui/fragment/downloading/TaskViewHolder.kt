package com.station.stationdownloader.ui.fragment.downloading

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.databinding.TaskItemBinding

class TaskViewHolder(val binding: TaskItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(taskItem: TaskItem){
        binding.taskItem=taskItem
    }

    companion object {
        fun create(parent: ViewGroup): TaskViewHolder {
            return TaskViewHolder(
                TaskItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }
}