package com.station.stationdownloader.ui.fragment.newtask

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.data.IResult

class FailedLinkAdapter : RecyclerView.Adapter<FailedLinkViewHolder>() {
    var failedLinks: List<Pair<String,IResult.Error>> = emptyList()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FailedLinkViewHolder {
        return FailedLinkViewHolder.create(parent)
    }

    override fun getItemCount(): Int {
        return failedLinks.size
    }

    override fun onBindViewHolder(holder: FailedLinkViewHolder, position: Int) {
        val failedLink = failedLinks[position]
        holder.bind(failedLink)
    }

    fun attachData(failedLinks: List<Pair<String,IResult.Error>>) {
        this.failedLinks = failedLinks
        notifyDataSetChanged()
    }
}