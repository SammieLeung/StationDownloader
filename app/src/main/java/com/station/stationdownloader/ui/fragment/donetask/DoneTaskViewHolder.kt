package com.station.stationdownloader.ui.fragment.donetask

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.station.stationdownloader.R
import com.station.stationdownloader.databinding.DoneTaskItemBinding
import com.station.stationkitkt.dp

class DoneTaskViewHolder(val binding: DoneTaskItemBinding, val accept: (UiAction) -> Unit) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(taskItem: DoneTaskItem) {
        binding.taskItem = taskItem
        binding.root.id = View.generateViewId()
        binding.root.setOnClickListener {
            accept(UiAction.OpenFile(taskItem.url))

        }
        binding.root.setOnLongClickListener {
            accept(UiAction.ShowTaskMenu(taskItem.url, true))
            true
        }
        binding.root.nextFocusLeftId = R.id.downloadedTaskItem
        if (this.absoluteAdapterPosition == 0) {
            binding.root.updateLayoutParams<RecyclerView.LayoutParams> {
                this.topMargin = 50.dp
            }
        } else {
            binding.root.updateLayoutParams<RecyclerView.LayoutParams> {
                this.topMargin = 0
            }
        }

        setUpNextFocus(bindingAdapter, binding.root)

    }

    private fun setUpNextFocus(bindingAdapter: RecyclerView.Adapter<out ViewHolder>?, targetView: View) {
        bindingAdapter?.let {
            if (this.bindingAdapterPosition == (it.itemCount - 1)) {
                targetView.nextFocusDownId = targetView.id
            } else {
                targetView.nextFocusDownId = View.NO_ID
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