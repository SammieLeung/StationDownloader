package com.station.stationdownloader.ui.fragment.newtask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.databinding.FailedLinkItemBinding

class FailedLinkViewHolder(val binding: FailedLinkItemBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(failedLink: Pair<String, IResult.Error>) {
        binding.failedLink=failedLink.first
        binding.root.isClickable=true
    }


    companion object{
        fun create(parent:ViewGroup):FailedLinkViewHolder{
            val binding = FailedLinkItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FailedLinkViewHolder(binding)
        }
    }
}