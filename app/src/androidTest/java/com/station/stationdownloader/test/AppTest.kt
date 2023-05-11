package com.station.stationdownloader.test

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.datasource.engine.aria2.WebSocketClient
import com.station.stationdownloader.data.datasource.IDownloadTaskRepository
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.di.MMKVModule
import com.station.stationdownloader.domain.Byte
import com.station.stationdownloader.domain.GB
import com.station.stationdownloader.domain.KB
import com.station.stationdownloader.domain.LoggerUseCase
import com.station.stationdownloader.domain.MB
import com.tencent.mmkv.MMKV
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

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
}