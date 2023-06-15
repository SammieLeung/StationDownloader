package com.station.stationdownloader.test

import androidx.annotation.WorkerThread
import androidx.core.util.asConsumer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.internal.notifyAll
import okhttp3.internal.wait
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    fun testTorrent() {
        Logger.addLogAdapter(AndroidLogAdapter())
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        XLTaskHelper.init(appContext)
        val torrentInfo =
            XLTaskHelper.instance().getTorrentInfo("sdcard/Station/torrent/test.torrent")
        Logger.d(torrentInfo.mMultiFileBaseFolder)
        Logger.d(torrentInfo.mInfoHash)
        Logger.d(torrentInfo.mIsMultiFiles)
        Logger.d(torrentInfo.mFileCount)

        val subFileInfo = torrentInfo.mSubFileInfo[47]
        Logger.d(subFileInfo.mFileIndex)
        Logger.d(subFileInfo.mFileName)
        Logger.d(subFileInfo.mFileSize)
        Logger.d(subFileInfo.mRealIndex)
        Logger.d("subPath=${subFileInfo.mSubPath}")
        Logger.d("hash=${subFileInfo.hash}")
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
}

fun Any.wait(timeout: Long) = (this as Object).wait(timeout)