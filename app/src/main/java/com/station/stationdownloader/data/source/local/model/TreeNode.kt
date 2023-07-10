package com.station.stationdownloader.data.source.local.model

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

        override fun autoSelect(select: Boolean) {
            isChecked = select
            parent.notifyChange(this)
        }
    }

    open class Directory(
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

                FolderCheckState.NONE -> autoSelect(true)
            }
        }


        override fun autoSelect(select: Boolean) {
            children.forEach {
                it.autoSelect(select)
            }
        }

        fun toggleFold() {
            isFold = !isFold
        }

    }

    object Root: Directory(
        folderName = "root",
        parent = null,
        deep = -1
    )

    protected abstract fun autoSelect(select: Boolean)

    abstract fun toggle()


    fun isRoot(): Boolean {
        return _parent == null && _deep == -1
    }

    enum class FolderCheckState {
        ALL, PART, NONE
    }
}
