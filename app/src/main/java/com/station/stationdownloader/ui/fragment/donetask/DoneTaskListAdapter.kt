package com.station.stationdownloader.ui.fragment.donetask

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.utils.DLogger

class DoneTaskListAdapter(val accept: (UiAction) -> Unit) :
    RecyclerView.Adapter<DoneTaskViewHolder>(), DLogger {
    private var dataList: MutableList<DoneTaskItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoneTaskViewHolder {
        return DoneTaskViewHolder.create(parent, accept)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: DoneTaskViewHolder, position: Int) {
        holder.bind(taskItem = dataList[position])
    }

    override fun DLogger.tag(): String {
        return DoneTaskListAdapter::class.java.simpleName
    }

    fun fillData(newList: List<DoneTaskItem>) {
        dataList.clear()
        dataList.addAll(newList)
        notifyDataSetChanged()
    }

    fun deleteTask(deleteTaskItem: DoneTaskItem) {
        val index = dataList.indexOf(deleteTaskItem)
        if (index != -1) {
            dataList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addNewTask(newTaskItem: DoneTaskItem) {
        dataList.add(newTaskItem)
        notifyItemInserted(dataList.size - 1)
    }

}