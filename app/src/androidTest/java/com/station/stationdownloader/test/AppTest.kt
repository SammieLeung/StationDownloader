package com.station.stationdownloader.test

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.utils.TaskTools
import com.station.stationdownloader.utils.TaskTools.toHumanReading
import com.station.stationdownloader.utils.asMB
import com.xunlei.downloadlib.XLTaskHelper
import com.xunlei.downloadlib.parameter.TorrentInfo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.internal.concurrent.Task
import okhttp3.internal.notifyAll
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AppTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)


    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.station.stationdownloader", appContext.packageName)
    }

    @Test
    fun testToHumanReading() {
        Logger.addLogAdapter(AndroidLogAdapter())
        Logger.d(String.format("%.2f", 1024 * 1024.asMB))

    }

    fun printFileTree(treeNode: TreeNode, indent:String="", tag:String="treeNode"){

        val prefix =  if (treeNode.isDirectory()) {
            val dir=treeNode as TreeNode.Directory
            when(dir.checkState){
                TreeNode.FolderCheckState.ALL -> " [√]"
                TreeNode.FolderCheckState.PART -> " [-]"
                TreeNode.FolderCheckState.NONE -> " [x]"
            }
        }  else {
            val file=treeNode as TreeNode.File
            if (file.isChecked)
                " {√} 【${file.fileIndex}】"
            else
                " {x} 【${file.fileIndex}】"
        }
        Log.d(tag,"$indent$prefix${treeNode._name}")
        if(treeNode.isDirectory() && treeNode.isFold()) {
            return
        }
        treeNode._children?.forEach {
            printFileTree(it, "$indent  ",tag)
        }
    }

    fun TorrentInfo.getFileTree(): TreeNode {
        val root =
            TreeNode.Directory("root", TreeNode.FolderCheckState.NONE, 0, mutableListOf(), null, -1)

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
    fun testTorrent() {
        Logger.addLogAdapter(AndroidLogAdapter())
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        XLTaskHelper.init(appContext)
        val torrentInfo =
            XLTaskHelper.instance().getTorrentInfo("sdcard/Station/tv.torrent")
        Logger.d(torrentInfo.mMultiFileBaseFolder)
        Logger.d(torrentInfo.mInfoHash)
        Logger.d(torrentInfo.mIsMultiFiles)
        Logger.d(torrentInfo.mFileCount)

        val treeNode=torrentInfo.getFileTree()
        (treeNode._children?.filter {
            it is TreeNode.Directory
        }?.get(1)?._children?.get(2) as TreeNode.File).toggle()
        printFileTree(treeNode)
        val TAG = "testTorrent"
        for (subfileInfo in torrentInfo.mSubFileInfo) {
            Log.d(
                TAG,
                "${subfileInfo.mFileIndex} Real[${subfileInfo.mRealIndex}] 【${subfileInfo.mFileName}】 ${subfileInfo.mSubPath.split("/")}【${
                    subfileInfo.mSubPath.split("/").size
                }】 ${subfileInfo.mFileSize.toHumanReading()}"
            )
        }



    }


    var re: InternalResponse? = null

    @WorkerThread
    private fun send() {
        re = InternalResponse()
        synchronized(re as InternalResponse) {
            println(1)
            re?.wait(1000)
            println(2)
            println(re?.getResult()?.get("a"))
            println(3)
        }
    }

    @Test
    fun testNotify() {

        Logger.addLogAdapter(AndroidLogAdapter())
        try {
            send()

        } catch (e: Exception) {
            Logger.d("test")
        }
        val b = Thread {
            println(21)
            re?.data(JSONObject().put("a", "b"))
            println(23)

        }

        Thread.sleep(2000)
        b.start()

    }


    interface FakeMessageCallback {
        suspend fun send(code: Int)
    }

    data class InternalResponse(
        @Volatile private var obj: JSONObject? = null,
        @Volatile private var exception: Exception? = null
    ) {
        @Synchronized
        fun data(json: JSONObject) {
            this.obj = json
            this.notifyAll()
        }

        @Synchronized
        fun failed(exception: Exception) {
            this.exception = exception
            notifyAll()
        }


        fun isSuccess(): Boolean {
            return this.obj != null
        }

        fun getResult(): JSONObject {
            return this.obj as JSONObject
        }

        fun getError(): Exception {
            return exception as Exception
        }

    }

    @Test
    fun testDecodeThunderLink() {
        println(TaskTools.thunderLinkDecode("thunder://QUFodHRwczovL3JlZGlyZWN0b3IuZ3Z0MS5jb20vZWRnZWRsL2FuZHJvaWQvc3R1ZGlvL2lkZS16aXBzLzIwMjIuMi4xLjIwL2FuZHJvaWQtc3R1ZGlvLTIwMjIuMi4xLjIwLWxpbnV4LnRhci5nelpa"))

    }

    fun Long.toHumanReading(): String {
        return toHumanReading(this)
    }

}



fun Any.wait(timeout: Long) = (this as Object).wait(timeout)