package com.station.stationdownloader.ui.fragment.downloading

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.utils.DLogger

class TaskListAdapter(val accept:(UiAction)->Unit) : RecyclerView.Adapter<TaskViewHolder>(), DLogger {
    private var dataList: MutableList<TaskItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder.create(parent,accept)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val taskItem=dataList[position]
        logger("onBindViewHodler ${taskItem.speed}")
        holder.bind(taskItem = dataList[position])
    }

    override fun DLogger.tag(): String {
        return TaskListAdapter::class.java.simpleName
    }

    fun fillData(newList: List<TaskItem>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    fun updateProgress(item: TaskItem) {
        for (idx in dataList.indices) {
            val data = dataList[idx]
            if (data.url == item.url) {
                dataList[idx] = item
                notifyItemChanged(idx)
                break;
            }
        }
    }

}