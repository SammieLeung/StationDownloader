package com.station.stationdownloader.ui.fragment.newtask

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.databinding.FileItemBinding

class TreeNodeAdapter(val root: TreeNode.Root) :
    RecyclerView.Adapter<TreeNodeAdapter.FileViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder.create(parent, viewType)
    }

    override fun getItemCount(): Int {
        return root.getChildrenCount()
    }


    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
    }

    class FileViewHolder(val binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(fileStateItem: TreeNode) {
            if (fileStateItem is TreeNode.File) {
                binding.fileName.setText(fileStateItem.fileName)
                binding.checkbox.isChecked = fileStateItem.isChecked
            }
        }


        companion object {
            fun create(parent: ViewGroup, viewType: Int): FileViewHolder {
                val binding = FileItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FileViewHolder(binding)
            }
        }
    }

    private fun TreeNode.Directory.getChildrenCount(): Int {
        var count = 0;
        _children?.forEach {
            if (it is TreeNode.File)
                count++
            else if (it is TreeNode.Directory) {
                if (it.isFold) {
                    count++
                } else {
                    count += it.getChildrenCount()
                    count++
                }
            }
        }
        return count
    }

    private fun TreeNode.Directory.findNodeByIndexRecursive(position: Int): TreeNode? {
        var currentPos = position
        for (child in children) {
            if (currentPos == 0) {
                return child
            }
            if (child is TreeNode.Directory) {
                val childNode = child.findNodeByIndexRecursive(currentPos - 1)
                if (childNode != null)
                    return childNode
                currentPos--
            } else if (child is TreeNode.File) {
                currentPos--
            }
        }
        return null
    }


}