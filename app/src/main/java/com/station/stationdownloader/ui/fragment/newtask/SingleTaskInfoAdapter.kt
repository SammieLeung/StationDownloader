package com.station.stationdownloader.ui.fragment.newtask

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.data.source.local.engine.MultiNewTaskConfigModel
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel

class SingleTaskInfoAdapter: RecyclerView.Adapter<SingleTaskInfoViewHolder>() {
    lateinit var multiTaskConfigModel: MultiNewTaskConfigModel

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SingleTaskInfoViewHolder {
      return  SingleTaskInfoViewHolder.create(parent)
    }

    override fun getItemCount(): Int {
        return multiTaskConfigModel.taskConfigs.size
    }

    override fun onBindViewHolder(holder:SingleTaskInfoViewHolder, position: Int) {
        val singleTaskConfigModel=multiTaskConfigModel.taskConfigs[position]
        holder.bind(singleTaskConfigModel)
    }

    fun attachData(multiTaskConfigModel: MultiNewTaskConfigModel){
        this.multiTaskConfigModel=multiTaskConfigModel
        notifyDataSetChanged()
    }
}