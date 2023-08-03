package com.station.stationdownloader.test

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Exception
import javax.inject.Inject

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
