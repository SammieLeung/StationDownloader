package com.station.stationdownloader.ui.fragment.downloading

import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.databinding.TaskItemBinding

class TaskViewHolder(val binding: TaskItemBinding): RecyclerView.ViewHolder(binding.root) {
    fun bind(taskItem: TaskItem){
            binding.bind(taskItem)
    }

    private fun TaskItemBinding.bind(taskItem: TaskItem){

    }
}