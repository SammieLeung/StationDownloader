package com.station.stationdownloader.test

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.model.TreeNode
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

    fun TorrentInfo.getFileTree(): TreeNode {
        val root =TreeNode.Root

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
                            isChecked = if (TaskTools.isMediaFile(comp)) true else false,
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
        treeNode.getByFileIndex(15)?.toggle()
        treeNode.getByFileIndex(2)?.toggle()
        treeNode.getByFileIndex(3)?.toggle()


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