package com.station.stationdownloader.data.source.local.model

import com.station.stationdownloader.FileType
import com.station.stationdownloader.utils.TaskTools

sealed class TreeNode(
    val _name: String,
    val _parent: TreeNode?,
    val _children: MutableList<TreeNode>?,
    val _deep: Int,
) {
    data class File(
        val fileIndex: Int,
        val fileName: String,
        val fileExt: String,
        val fileSize: Long,
        var isChecked: Boolean = false,
        val parent: Directory,
        val deep: Int
    ) : TreeNode(
        _name = fileName,
        _parent = parent,
        _children = null,
        _deep = deep
    ) {
        override fun toggle() {
            autoSelect(!isChecked)
        }

        override fun isRoot(): Boolean {
            return false
        }

        override fun autoSelect(select: Boolean) {
            if (isChecked != select) {
                isChecked = select
                parent.notifyChange(this)
            }
        }

        override fun toString(): String {
            return "File>>$fileName"
        }

        fun isVideo(): Boolean {
            return TaskTools.isVideoFile(fileExt)
        }

        fun isAudio(): Boolean {
            return TaskTools.isAudioFile(fileExt)
        }

        fun isImage(): Boolean {
            return TaskTools.isImageFile(fileExt)
        }

        fun isCompress(): Boolean {
            return TaskTools.isCompress(fileExt)
        }

        fun toHumanReadingFileSize(): String {
            return TaskTools.toHumanReading(fileSize)
        }
    }

    data class Directory(
        val folderName: String,
        var checkState: FolderCheckState = FolderCheckState.NONE,
        var totalCheckedFileSize: Long = 0,
        var checkedFileCount: Int = 0,
        var totalFileCount: Int = 0,
        val children: MutableList<TreeNode> = mutableListOf(),
        val parent: Directory?,
        val deep: Int,
        var isFold: Boolean = false
    ) : TreeNode(
        _name = folderName,
        _parent = parent,
        _children = children,
        _deep = deep
    ) {
        /**
         * 创建文件树时调用
         */
        fun addChild(treeNode: TreeNode) {
            if (_children != null) {
                if (treeNode is File) {
                    syncAddChildState(treeNode)
                }
                _children.add(treeNode)
            }
        }

        private fun syncAddChildState(treeNode: File) {
            if (treeNode.isChecked) {
                totalCheckedFileSize += treeNode.fileSize
                checkedFileCount += 1
            }
            totalFileCount += 1
            if (checkedFileCount == 0) {
                checkState = FolderCheckState.NONE
            } else if (checkedFileCount == totalFileCount) {
                checkState = FolderCheckState.ALL
            } else {
                checkState = FolderCheckState.PART
            }
            parent?.syncAddChildState(treeNode)
        }

        fun getByFileIndex(fileIndex: Int): TreeNode? {
            return _children?.let {
                var treeNode: TreeNode? = null
                for (node in it) {
                    if (node is File && node.fileIndex == fileIndex) {
                        treeNode = node
                        break;
                    } else if (node is Directory) {
                        treeNode = node.getByFileIndex(fileIndex)
                        if (treeNode != null)
                            break
                    }
                }
                return treeNode
            }

        }

        fun notifyChange(treeNode: File) {
            if (treeNode.isChecked) {
                totalCheckedFileSize += treeNode.fileSize
                checkedFileCount += 1
                if (totalFileCount == checkedFileCount)
                    checkState = FolderCheckState.ALL
                else
                    checkState = FolderCheckState.PART
            } else {
                totalCheckedFileSize -= treeNode.fileSize
                checkedFileCount -= 1
                if (checkedFileCount == 0)
                    checkState = FolderCheckState.NONE
                else
                    checkState = FolderCheckState.PART
            }
            parent?.notifyChange(treeNode)
        }

        override fun toggle() {
            when (checkState) {
                FolderCheckState.ALL -> autoSelect(false)

                FolderCheckState.PART -> autoSelect(true)
                //FIXME 这里已被选的文件会再次计算
                FolderCheckState.NONE -> autoSelect(true)
            }
        }

        override fun isRoot(): Boolean {
            return folderName == ROOT && _parent == null && _deep == -1
        }


        override fun autoSelect(select: Boolean) {
            children.forEach {
                it.autoSelect(select)
            }
        }

        override fun toString(): String {
            return "Dir>>$folderName"
        }

        fun toggleFold() {
            isFold = !isFold
        }

        fun toHumanReadingSelectSize(): String {
            return TaskTools.toHumanReading(totalCheckedFileSize)
        }

        companion object {
            fun createRoot(): Directory {
                return Directory(
                    folderName = ROOT,
                    parent = null,
                    deep = -1
                )
            }
        }

    }

    abstract fun autoSelect(select: Boolean)

    abstract fun toggle()


    abstract fun isRoot(): Boolean

    enum class FolderCheckState {
        ALL, PART, NONE
    }

    companion object {
        private const val ROOT = "root"
    }
}

public fun TreeNode.Directory.getChildrenCount(): Int {
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


public fun TreeNode.Directory.findNodeByIndexRecursive(position: Int): TreeNode? {
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

public fun TreeNode.Directory.getChildAbsolutionPosition(treeNode: TreeNode): Int {
    var currentPos = -1
    for (idx in 0 until getChildrenCount()) {
        val child = children[idx]
        if (child == treeNode)
            return idx
        if (child is TreeNode.Directory) {
            val childPos = child.getChildAbsolutionPosition(treeNode)
            if (childPos != -1) {
                return idx + childPos + 1
            } else {
                currentPos += child.getChildrenCount() + 1
            }
        }
    }
    return currentPos
}

public fun TreeNode.getParenPosition(pos: Int): Int {
    if (_parent != null) {
        if (_parent._children != null)
            return pos - _parent._children.indexOf(this) - 1
    }
    return -1
}

public fun TreeNode.Directory.filterFile(fileType: FileType, isSelect: Boolean) {

    children.forEach {
        when (it) {
            is TreeNode.Directory -> {
                it.filterFile(fileType, isSelect)
            }

            is TreeNode.File -> {
                when (fileType) {
                    FileType.VIDEO -> {
                        if (it.isVideo()) {
                            it.autoSelect(isSelect)
                        }
                    }

                    FileType.AUDIO -> {
                        if (it.isAudio()) {
                            it.autoSelect(isSelect)
                        }
                    }

                    FileType.IMG -> {
                        if (it.isImage()) {
                            it.autoSelect(isSelect)
                        }
                    }

                    FileType.OTHER -> {
                        if (!(it.isAudio() || it.isVideo() || it.isImage())) {
                            it.autoSelect(isSelect)
                        }
                    }
                }
            }
        }
    }

}