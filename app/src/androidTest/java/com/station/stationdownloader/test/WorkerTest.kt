package com.station.stationdownloader.test

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadWorker
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CancellationException

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class WorkerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Before
    fun setUp() {
        Logger.addLogAdapter(AndroidLogAdapter())
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSleepWorker() {
        val url = "sdcard/Station/tvset2.torrent"
        val engine = "XL"
        val downloadPath = "/storage/emulated/0/Station/Download/torrent"
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

    @Test
    fun testTaskRunnable() {
        runBlocking {
            TaskRunnable().speedTest().join()
        }
    }


    class TaskRunnable() : Runnable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var job: Job? = null
        var retryFailedCount = 0
        var speedTestData = listOf(0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 7, 9)


        override fun run() {
            job = speedTest()
        }

        fun speedTest(retryFailedCount: Int = 0, delayTestTime: Int = 30) =
            scope.launch {
                var count = 0
                var delayCount = delayTestTime
                var nullCount = 0
                var startSpeedTest = false
                var retryCount = retryFailedCount
                var noSpeedCount = 0

                while (isActive) {
                    val taskInfo = 1
                    if (taskInfo == null) {
                        if (nullCount < 5) {
                            nullCount++
                            Logger.d("nullCount==$nullCount")
                            delay(1000)
                            continue
                        }
                        cancel(CancellationException("taskInfo is null"))
                    }
                    nullCount = 0

                    if (delayCount > 0) {
                        Logger.d("delayCount=$delayCount")
                        delayCount--
                    } else {
                        startSpeedTest = true
                    }




                    val speed = if (count < speedTestData.size) speedTestData.get(count) else 0
                    count++
                    Logger.d("speed=$speed")
                    if (speed <= 0) {
                        if (startSpeedTest)
                            noSpeedCount++
                    } else {
                        delayCount = 0
                        retryCount = 0
                        noSpeedCount = 0
                    }
                    Logger.d("noSpeedCount=$noSpeedCount")

                    if (noSpeedCount > 5) {
                        if (retryCount > 10) {
                            Logger.d("retryCount=$retryCount")
                            cancel(CancellationException("try failed too much"))
                        }
                        restartTask(retryCount,5*(retryCount))
                        cancel()
                    }

                    delay(1000)
                }
            }

        private suspend fun restartTask(retryCount: Int, delayTestTime: Int) {
            Logger.d("restartTask[retryCount]=$retryCount")
            job = speedTest(retryCount + 1,delayTestTime)
        }

    }
}