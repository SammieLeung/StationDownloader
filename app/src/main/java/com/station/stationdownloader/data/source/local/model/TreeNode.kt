package com.station.stationdownloader.data.source.local.model

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
        override fun manualSelect(select: Boolean) {
            autoSelect(select)
            parent.updateState()
        }

        override fun autoSelect(select: Boolean) {
            isChecked = select
        }

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
                updateState()
            }
        }

        fun updateState() {
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
            parent?.updateState()
        }

        fun toggle(){
            when(checkState){
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
            parent?.updateState()
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
    }

    protected abstract fun manualSelect(select: Boolean)

    protected abstract fun autoSelect(select: Boolean)

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
