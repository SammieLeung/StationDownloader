package com.station.stationdownloader.ui.fragment.newtask

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.R
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.getParenPosition
import com.station.stationdownloader.databinding.FileItemBinding
import com.station.stationdownloader.ui.fragment.newtask.TreeNodeAdapter.Companion.TREE_NODE_INDENT
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.view.ThreeStateCheckbox
import com.station.stationkitkt.dp

class FileViewHolder(val binding: FileItemBinding) :
    RecyclerView.ViewHolder(binding.root), DLogger {
    init {
        binding.root.setOnClickListener {
            it.findViewById<ThreeStateCheckbox>(R.id.checkbox).performClick()
        }
    }

    fun bind(treeNode: TreeNode.File, position: Int) {
        binding.root.setOnFocusChangeListener(null)
        binding.checkbox.setOnStateChangeListener(null)
        binding.node = treeNode
        binding.checkbox.setState(treeNode.isChecked)
        addIndent(treeNode.deep)
        binding.root.setOnFocusChangeListener { v, hasFocus ->
            v.isSelected = hasFocus
        }
        binding.checkbox.setOnStateChangeListener(object :
            ThreeStateCheckbox.OnStateChangeListener {
            override fun onStateChanged(
                checkbox: ThreeStateCheckbox,
                newState: Boolean?
            ) {

                if(newState!=null){
                    checkbox.post {
                        treeNode.autoSelect(newState)
                        notifyParent(treeNode,position)
                    }
                }

//                logger("${treeNode.fileName} $newState")
            }
        })

    }

    fun notifyParent(node:TreeNode?,pos:Int){
        if(node!=null&&node._parent !is TreeNode.Root) {
            val parentPos = node.getParenPosition(pos)
            bindingAdapter?.notifyItemChanged(parentPos)
            notifyParent(node._parent, parentPos)
        }
    }

    private fun addIndent(deep: Int) {
        binding.itemView.setPadding(
            (deep * TREE_NODE_INDENT).dp + 20.dp,
            0,
            20.dp,
            0
        )
    }

    companion object {

        fun create(parent: ViewGroup): FileViewHolder {
            return FileViewHolder(
                FileItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        }
    }

    override fun DLogger.tag(): String {
        return this.javaClass.simpleName
    }
}