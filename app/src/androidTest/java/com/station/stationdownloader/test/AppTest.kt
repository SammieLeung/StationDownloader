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