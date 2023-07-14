package com.station.stationdownloader.ui.fragment.newtask

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.orhanobut.logger.Logger
import com.station.stationdownloader.R
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.findNodeByIndexRecursive
import com.station.stationdownloader.data.source.local.model.getChildrenCount
import com.station.stationdownloader.databinding.FileItemBinding
import com.station.stationdownloader.databinding.FolderItemBinding
import com.station.stationdownloader.utils.DLogger
import com.station.stationdownloader.utils.TaskTools
import com.station.stationdownloader.view.ThreeStateCheckbox
import com.station.stationkitkt.dp

class TreeNodeAdapter :
    RecyclerView.Adapter<TreeNodeAdapter.FileViewHolder>(), DLogger {
    lateinit var root: TreeNode.Root
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        return FileViewHolder.create(parent, viewType)
    }

    override fun getItemCount(): Int {
        val count= root.getChildrenCount()
        return count
    }

    override fun getItemViewType(position: Int): Int {
        return root.findNodeByIndexRecursive(position)?.let {
            if (it is TreeNode.File)
                return FileViewHolder.TYPE_FILE
            else
                return FileViewHolder.TYPE_DIRECTORY
        } ?: FileViewHolder.TYPE_FILE
    }


    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val treeNode = root.findNodeByIndexRecursive(position)
        if (treeNode != null) {
            holder.bind(treeNode,position)
        }
    }

    fun fillData(root: TreeNode.Root) {
        this.root = root
        printFileTree(root)
        notifyDataSetChanged()
    }




    class FileViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(treeNode: TreeNode, position: Int) = when (treeNode) {
            is TreeNode.Directory -> {
                val folderItemBinding = this.binding as FolderItemBinding
                folderItemBinding.node = treeNode
                folderItemBinding.itemView.setPadding(
                    (treeNode.deep * TREE_NODE_INDENT).dp + 20.dp,
                    0,
                    20.dp,
                    0
                )
                when (treeNode.checkState) {
                    TreeNode.FolderCheckState.ALL ->
                        folderItemBinding.checkbox.setState(true)

                    TreeNode.FolderCheckState.PART ->
                        folderItemBinding.checkbox.setState(null)

                    TreeNode.FolderCheckState.NONE ->
                        folderItemBinding.checkbox.setState(false)
                }
                val folder: TreeNode.Directory = treeNode
                folderItemBinding.checkbox.setOnStateChangeListener(object :
                    ThreeStateCheckbox.OnStateChangeListener {
                    override fun onStateChanged(
                        checkbox: ThreeStateCheckbox,
                        newState: Boolean?
                    ) {
                        if (newState != null) {
                            folder.autoSelect(newState)
//                            notify UI
                            checkbox.post{
                                bindingAdapter?.notifyItemRangeChanged(position+1,folder.getChildrenCount())
                            }
                        }
                    }
                })
                folderItemBinding.root.setOnClickListener {
                    it.findViewById<ThreeStateCheckbox>(R.id.checkbox).performClick()
                }
            }

            is TreeNode.File -> {
                val fileItemBinding = this.binding as FileItemBinding
                fileItemBinding.node = treeNode
                fileItemBinding.itemView.setPadding(
                    (treeNode.deep * TREE_NODE_INDENT).dp + 20.dp,
                    0,
                    20.dp,
                    0
                )
                fileItemBinding.checkbox.setOnStateChangeListener(object :
                    ThreeStateCheckbox.OnStateChangeListener {
                    override fun onStateChanged(
                        checkbox: ThreeStateCheckbox,
                        newState: Boolean?
                    ) {
                        Logger.d("${treeNode.fileName} $newState")
                    }
                })
                fileItemBinding.root.setOnClickListener {
                    it.findViewById<ThreeStateCheckbox>(R.id.checkbox).performClick()
                }
            }
        }


        companion object {
            const val TREE_NODE_INDENT = 30
            const val TYPE_DIRECTORY = 1
            const val TYPE_FILE = 0
            fun create(parent: ViewGroup, viewType: Int): FileViewHolder {
                return if (viewType == TYPE_DIRECTORY) {
                    FileViewHolder(
                        FolderItemBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )
                    )
                } else {
                    FileViewHolder(
                        FileItemBinding.inflate(
                            LayoutInflater.from(parent.context),
                            parent,
                            false
                        )
                    )
                }
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