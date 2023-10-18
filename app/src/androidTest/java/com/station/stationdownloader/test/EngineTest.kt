package com.station.stationdownloader.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class EngineTest {


    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
        Logger.addLogAdapter(AndroidLogAdapter())

    }

    @After
    fun uninit() {
        XLTaskHelper.uninit()
    }

    @Test
    fun testAdd() {
        var a: Double = 0.0
        val c = 1.2
        for (i in 1..29) {
            a = a * 1.03 + c
        }
        println("data $a")
    }

    @Test
    fun testXLEngine(){
        XLTaskHelper.init(ApplicationProvider.getApplicationContext())
       val id= XLTaskHelper.instance().addTorrentTask("/storage/emulated/0/Station/tvset2.torrent","/storage/emulated/0/Station/Test",
            intArrayOf()
        )
        Logger.d("id=$id")
    }
}


class StatusWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L)
        while (true) {
            val info = XLTaskHelper.instance().getTaskInfo(taskId)
            Logger.d("doWork  ${info.mDownloadSpeed} ${info.mDownloadSize} ${info.mFileSize}")
            if (info.mFileSize > 0) {
                setProgress(workDataOf("progress" to (info.mDownloadSize * 1.0 / info.mFileSize)))
            }
            delay(1000)
        }
        return Result.success()
    }
}
