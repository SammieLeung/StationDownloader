package com.station.stationdownloader.test

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.getCheckedFilePaths
import com.station.stationdownloader.data.source.local.model.getFilePaths
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.TorrentInfo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TorrentTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)


    @Before
    fun init() {
        hiltRule.inject()
        Logger.addLogAdapter(AndroidLogAdapter())
    }

    @Test
    fun testTorrent() {
        getTorrentInfo("sdcard/Station/tvset2.torrent")
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

    fun TorrentInfo.getFileTree(): TreeNode {
        val root = TreeNode.Directory.createRoot()

        for (fileInfo in mSubFileInfo) {
            val filePath =
                File(fileInfo.mSubPath, fileInfo.mFileName).path
            val pathComponents = filePath.split(File.separator)
            var currentNode: TreeNode.Directory = root
            for (idx in pathComponents.indices) {
                val comp = pathComponents[idx]
                val isFile = pathComponents.lastIndex == idx
                val existsChild: TreeNode.Directory? = currentNode.children.find {
                    it is TreeNode.Directory && it.folderName == comp
                } as TreeNode.Directory?

                if (existsChild != null) {
                    currentNode = existsChild
                } else {
                    if (isFile) {
                        val newChild = TreeNode.File(
                            fileInfo.mRealIndex,
                            comp,
                            TaskTools.getExt(comp),
                            fileInfo.mFileSize,
                            isChecked = if (TaskTools.isVideoFile(comp)) true else false,
                            parent = currentNode,
                            deep = idx
                        )
                        currentNode.addChild(newChild)
                    } else {
                        val newChild = TreeNode.Directory(
                            comp,
                            TreeNode.FolderCheckState.NONE,
                            0,
                            0,
                            children = mutableListOf(),
                            parent = currentNode,
                            deep = idx
                        )
                        currentNode.addChild(newChild)
                        currentNode = newChild
                    }
                }
            }
        }
        return root
    }

    @Test
    fun testEmpty(){
       val data=""
        if(data.isEmpty()) {
            Logger.d("1")
        }

        val data2:String?=null
        if(data2.isNullOrEmpty()) {
            Logger.d("2")
        }
    }


    fun getTorrentInfo(url: String) {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        XLTaskHelper.init(appContext)
        val torrentInfo =
            XLTaskHelper.instance().getTorrentInfo(url)
        Logger.d(torrentInfo.mMultiFileBaseFolder)
        Logger.d(torrentInfo.mInfoHash)
        Logger.d(torrentInfo.mIsMultiFiles)
        Logger.d(torrentInfo.mFileCount)


        val treeNode = torrentInfo.getFileTree() as TreeNode.Directory
        treeNode.getByFileIndex(1)?.autoSelect(true)
        treeNode.getByFileIndex(3)?.autoSelect(true)
        treeNode.getByFileIndex(5)?.autoSelect(true)



        treeNode.getCheckedFilePaths().forEach {
            Logger.d(it)
        }



        printFileTree(treeNode)
//        val TAG = "testTorrent"
//        for (subfileInfo in torrentInfo.mSubFileInfo) {
//            Log.d(
//                TAG,
//                "${subfileInfo.mFileIndex} Real[${subfileInfo.mRealIndex}] 【${subfileInfo.mFileName}】 ${
//                    subfileInfo.mSubPath.split(
//                        "/"
//                    )
//                }【${
//                    subfileInfo.mSubPath.split("/").size
//                }】 ${subfileInfo.mFileSize.toHumanReading()}"
//            )
//        }

    }

    fun Long.toHumanReading(): String {
        return TaskTools.toHumanReading(this)
    }

}