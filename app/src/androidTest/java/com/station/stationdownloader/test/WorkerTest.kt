package com.station.stationdownloader.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerBuilder
import androidx.work.workDataOf
import com.station.stationdownloader.DownloadWorker
import com.station.stationdownloader.data.source.IDownloadTaskRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WorkerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSleepWorker() {
        val url="sdcard/Station/tvset2.torrent"
        val engine="XL"
        val downloadPath="/storage/emulated/0/Station/Download/torrent"
        val worker = TestListenableWorkerBuilder<DownloadWorker>(context).setInputData(
            workDataOf(
                DownloadWorker.IN_DOWNLOAD_PATH to downloadPath,
                DownloadWorker.IN_ENGINE to engine,
                DownloadWorker.IN_URL to url
            )
        ).build()

        runBlocking {
            worker.doWork()
        }
    }
}