package com.station.stationdownloader.ui.fragment.newtask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.R
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.findNodeByIndexRecursive
import com.station.stationdownloader.data.source.local.model.getChildrenCount
import com.station.stationdownloader.databinding.FolderItemBinding
import com.station.stationdownloader.ui.fragment.newtask.TreeNodeAdapter.Companion.TREE_NODE_INDENT
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.view.ThreeStateCheckbox
import com.station.stationkitkt.dp

class DirectoryViewHolder(
    val binding: FolderItemBinding,
) : RecyclerView.ViewHolder(binding.root),
    DLogger {
    val checkBox = binding.checkbox

    init {
        binding.root.setOnClickListener {
            it.findViewById<ThreeStateCheckbox>(R.id.checkbox).performClick()
        }
    }

    fun bind(treeNode: TreeNode.Directory, position: Int) {

        binding.node = treeNode
        addIndent(treeNode.deep)
        checkBox.setOnStateChangeListener(null)
        when (treeNode.checkState) {
            TreeNode.FolderCheckState.ALL ->
                checkBox.setState(true)

            TreeNode.FolderCheckState.PART ->
                checkBox.setState(null)

            TreeNode.FolderCheckState.NONE ->
                checkBox.setState(false)
        }
        checkBox.setOnStateChangeListener(object :
            ThreeStateCheckbox.OnStateChangeListener {
            override fun onStateChanged(
                checkbox: ThreeStateCheckbox,
                newState: Boolean?
            ) {
                treeNode.toggle()
                checkbox.post {
                    bindingAdapter?.notifyItemRangeChanged(
                        position,
                        treeNode.getChildrenCount() + 1
                    )
                }
            }
        })


    }

    private fun addIndent(deep: Int) {
        binding.itemView.setPadding(
            (deep * TREE_NODE_INDENT).dp + 20.dp,
            0,
            20.dp,
            0
        )
    }

    private fun notifyParentChange(treeNode: TreeNode) {
        if (treeNode._parent != null) {

        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
        ): DirectoryViewHolder {
            return DirectoryViewHolder(
                FolderItemBinding.inflate(
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