package com.station.stationdownloader.data.source.local.model

import okhttp3.internal.notify

sealed class TreeNode(
    val _name: String,
    val _parent: TreeNode?,
    val _children: MutableList<TreeNode>?,
    val _deep: Int
) {

    data class File(
        val fileIndex: Int,
        val fileName: String,
        val fileExt: String,
        val fileSize: Long,
        var isChecked: Boolean = false,
        val parent: Directory,
        val deep: Int
    ) : TreeNode(fileName, parent, null, deep) {
        fun toggle() {
            manualSelect(!isChecked)
        }

        override fun manualSelect(select: Boolean) {
            autoSelect(select)
            parent.updateAllCheckState()
        }

        override fun autoSelect(select: Boolean) {
            isChecked = select
        }

        override fun getSelectSize(): Long = if (isChecked) this.fileSize else 0L

        override fun getSelectCount(): Int = if (isChecked) 1 else 0
    }

    data class Directory(
        val folderName: String,
        var checkState: FolderCheckState,
        var totalSize: Long,
        val children: MutableList<TreeNode>,
        val parent: Directory?,
        val deep: Int
    ) : TreeNode(folderName, parent, children, deep) {
        fun addChild(treeNode: TreeNode) {
            if (_children != null) {
                _children.add(treeNode)
                updateAllCheckState()
                updateTotalSize()
            }
        }

        fun updateAllCheckState() {
            if (_children?.isNotEmpty() == true) {
                val allSelect = _children.none {
                    it is File && !it.isChecked || it is Directory && it.checkState != FolderCheckState.ALL
                }
                val allUnSelect = _children.none {
                    it is File && it.isChecked || it is Directory && it.checkState != FolderCheckState.NONE
                }

                checkState = if (allSelect && !allUnSelect) {
                    FolderCheckState.ALL
                } else if (!allSelect && allUnSelect) {
                    FolderCheckState.NONE
                } else {
                    FolderCheckState.PART
                }
            }
            parent?.updateAllCheckState()
        }

        fun updateTotalSize() {
//            parent?.updateTotalSize()
        }

        fun toggle() {
            when (checkState) {
                FolderCheckState.ALL ->
                    manualSelect(false)

                FolderCheckState.PART ->
                    manualSelect(true)

                FolderCheckState.NONE ->
                    manualSelect(true)
            }
        }

        override fun manualSelect(select: Boolean) {
            autoSelect(select)
            parent?.updateAllCheckState()
        }

        override fun autoSelect(select: Boolean) {
            if (select) {
                checkState = FolderCheckState.ALL
            } else {
                checkState = FolderCheckState.NONE
            }
            children.forEach {
                it.autoSelect(select)
            }
        }

        override fun getSelectSize(): Long {
            var totalSize = 0L
            _children?.forEach {
                totalSize += it.getSelectSize()
            }
            return totalSize
        }

        override fun getSelectCount(): Int {
            var count = 0
            _children?.forEach {
                count += it.getSelectCount()
            }
            return count
        }
    }

    protected abstract fun manualSelect(select: Boolean)

    protected abstract fun autoSelect(select: Boolean)

    protected abstract fun getSelectSize(): Long

    protected abstract fun getSelectCount(): Int

    fun isFile(): Boolean {
        return _children == null
    }

    fun isDirectory(): Boolean {
        return _children != null
    }

    fun isFold(): Boolean {
        return false
    }

    fun isRoot(): Boolean {
        return _parent == null && _deep == -1
    }

    enum class FolderCheckState {
        ALL, PART, NONE
    }
}
