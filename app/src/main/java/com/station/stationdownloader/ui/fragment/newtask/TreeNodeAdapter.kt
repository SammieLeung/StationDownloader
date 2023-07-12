package com.station.stationdownloader.ui.fragment.newtask

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.databinding.FileItemBinding
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import com.station.stationkitkt.dp

class TreeNodeAdapter :
    RecyclerView.Adapter<TreeNodeAdapter.FileViewHolder>(),DLogger {
    lateinit var root: TreeNode.Root
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder.create(parent, viewType)
    }

    override fun getItemCount(): Int {
        return root.getChildrenCount()
    }


    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val treeNode = root.findNodeByIndexRecursive(position)
        logger("【$position】[${treeNode?._name}]")
        if (treeNode != null) {
            holder.bind(treeNode)
        }

    }

    fun fillData(root: TreeNode.Root) {
        this.root = root
        printFileTree(root)
        notifyDataSetChanged()
    }


    private fun TreeNode.Directory.getChildrenCount(): Int {
        var count = 0;
        _children?.forEach {
            if (it is TreeNode.File)
                count++
            else if (it is TreeNode.Directory) {
                count += it.getChildrenCount()
                count++
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
                currentPos -= child.getChildrenCount() + 1
            } else if (child is TreeNode.File) {
                currentPos--
            }
        }
        return null
    }

    class FileViewHolder(val binding: FileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(treeNode: TreeNode) {
            when (treeNode) {
                is TreeNode.Directory -> {
                    binding.fileName.setText(treeNode.folderName)
                    binding.checkbox.setState(null)

                    binding.root.setPadding((treeNode.deep * 20).dp,  binding.root.paddingTop,  binding.root.paddingRight,  binding.root.paddingBottom)
//                    when (treeNode.checkState) {
//                        TreeNode.FolderCheckState.ALL ->
//                            binding.checkbox.setState(true)
//
//                        TreeNode.FolderCheckState.PART ->
//                            binding.checkbox.setState(null)
//
//                        TreeNode.FolderCheckState.NONE ->
//                            binding.checkbox.setState(false)
//                    }
                }

                is TreeNode.File -> {
                    binding.fileName.setText(treeNode.fileName)
                    binding.checkbox.isChecked = treeNode.isChecked
                    binding.root.setPadding((treeNode.deep * 20).dp,  binding.root.paddingTop,  binding.root.paddingRight,  binding.root.paddingBottom)
                }
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

    override fun DLogger.tag(): String {
        return "TreeNodeAdapter"
    }
}

fun printFileTree(treeNode: TreeNode, indent: String = "", tag: String = "treeNode") {

    val prefix = if (treeNode is TreeNode.Directory) {
        val dir = treeNode
        when (dir.checkState) {
            TreeNode.FolderCheckState.ALL -> " [√]"
            TreeNode.FolderCheckState.PART -> " [-]"
            TreeNode.FolderCheckState.NONE -> " [x]"
        }
    } else {
        val file = treeNode as TreeNode.File
        if (file.isChecked)
            " {√} 【${file.fileIndex}】"
        else
            " {x} 【${file.fileIndex}】"
    }
    val fileSize =
        if (treeNode is TreeNode.File) treeNode.fileSize else (treeNode as TreeNode.Directory).totalCheckedFileSize
    Log.d(tag, "$indent$prefix${treeNode._name} ${fileSize.toHumanReading()}")
    if (treeNode is TreeNode.Directory && treeNode.isFold) {
        return
    }
    treeNode._children?.forEach {
        printFileTree(it, "$indent  ", tag)
    }
}

fun Long.toHumanReading() = TaskTools.toHumanReading(this)