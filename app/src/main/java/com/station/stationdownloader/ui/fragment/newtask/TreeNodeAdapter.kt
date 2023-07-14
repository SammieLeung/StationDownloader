package com.station.stationdownloader.ui.fragment.newtask

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.station.stationdownloader.R
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.findNodeByIndexRecursive
import com.station.stationdownloader.data.source.local.model.getChildrenCount
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools

class TreeNodeAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>(), DLogger {
    lateinit var root: TreeNode.Root

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == R.layout.file_item) {
            FileViewHolder.create(parent)
        } else {
            DirectoryViewHolder.create(parent)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val treeNode = root.findNodeByIndexRecursive(position)
        when (treeNode) {
            is TreeNode.File -> (holder as FileViewHolder).bind(treeNode,position)
            is TreeNode.Directory -> (holder as DirectoryViewHolder).bind(treeNode,position)
            else -> {}
        }


    }

    override fun getItemCount(): Int {
        return root.getChildrenCount()
    }

    override fun getItemViewType(position: Int): Int {
        return root.findNodeByIndexRecursive(position)?.let {
            if (it is TreeNode.File) return R.layout.file_item
            else return R.layout.folder_item
        } ?: return R.layout.file_item
    }


    fun fillData(root: TreeNode.Root) {
        this.root = root
        printFileTree(root)
        notifyDataSetChanged()
    }


    override fun DLogger.tag(): String {
        return "TreeNodeAdapter"
    }

    companion object {
        const val TREE_NODE_INDENT = 30
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
        if (file.isChecked) " {√} 【${file.fileIndex}】"
        else " {x} 【${file.fileIndex}】"
    }
    val fileSize =
        if (treeNode is TreeNode.File) treeNode.fileSize else (treeNode as TreeNode.Directory).totalCheckedFileSize
    Log.d(tag, "$indent$prefix${treeNode._name} ${fileSize.toHumanReading()} ${if(treeNode is TreeNode.File) treeNode.isVideo() else ""}")
    if (treeNode is TreeNode.Directory && treeNode.isFold) {
        return
    }
    treeNode._children?.forEach {
        printFileTree(it, "$indent  ", tag)
    }
}

fun Long.toHumanReading() = TaskTools.toHumanReading(this)