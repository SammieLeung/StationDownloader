package com.station.stationdownloader.ui.fragment.downloading

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.utils.DLogger

class TaskListAdapter() : RecyclerView.Adapter<TaskViewHolder>(), DLogger {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun DLogger.tag(): String {
        return TaskListAdapter::class.java.simpleName
    }
}