package com.station.stationdownloader.test

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.await
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.TestWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class EngineTest {

    private lateinit var context: Context

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        XLTaskHelper.init(appContext)
        Logger.addLogAdapter(AndroidLogAdapter())
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun uninit() {
        XLTaskHelper.uninit()
    }

    @Inject
    lateinit var mEngineRepo: IEngineRepository

    @Test
    fun testGetSize() {
        runBlocking {
            coroutineScope {
                Logger.i("coroutineScope")
                launch {
                    flow {
                        val result = mEngineRepo
                            .initTask("magnet:?xt=urn:btih:c9997e77250a42e2ca912d1842e1727a34fbc295&dn=%5Bmp4kan%5Dd%E8%88%8Cl%E5%B8%88.2023.HD1080p.%E7%B2%A4%E8%AF%AD%E4%B8%AD%E5%AD%97.v2.mp4")
                        when (result) {
                            is IResult.Error -> {
                                throw result.exception
                            }

                            is IResult.Success -> {
                                emit(result.data)
                            }
                        }
                    }.catch {
                        Logger.d(it)
                    }.map {
                        val result = mEngineRepo.getTaskSize(
                            startDownloadTask = it,
                            timeOut = 30000
                        )
                        when (result) {
                            is IResult.Error -> {
                                throw result.exception
                            }

                            is IResult.Success -> {
                                result.data
                            }
                        }
                    }.catch {
                        Logger.d(it)
                    }.map {
                        val result = mEngineRepo.startTask(it.url,it.engine,it.downloadPath,it.name,it.urlType,it.fileCount,it.selectIndexes.toIntArray())
                        when (result) {
                            is IResult.Error -> {
                                throw result.exception
                            }

                            is IResult.Success -> {
                                result.data
                            }
                        }
                    }.catch {
                        Logger.d(it)
                    }.collect {
                        Logger.d("start taskId $it")

                        val request = OneTimeWorkRequestBuilder<StatusWorker>()
                            .setInputData(workDataOf("taskId" to it))
                            .build()
                        val workManager = WorkManager.getInstance(context)

                        workManager.enqueue(request)
                        val workInfo = workManager.getWorkInfoById(request.id).get()
                        delay(3000)
                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
                        delay(3000)
                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
                        delay(3000)
                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
                        WorkManager.getInstance(context).cancelAllWorkByTag("runningTask")
                        delay(3000)
                        Logger.d("workInfo ${workInfo.state}")
                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
                        delay(3000)
                        Logger.d("workInfo ${workInfo.state}")
                        delay(100000)

                    }

                }
            }

        }
        Logger.i("runBlocking")
    }
}

@Test
fun testJob() {

}

class StatusWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong("taskId", -1L)
        while (true) {
            val info = XLTaskHelper.instance().getTaskInfo(taskId)
            Logger.d("doWork  ${info.mDownloadSpeed} ${info.mDownloadSize} ${info.mFileSize}")
            if(info.mFileSize>0) {
                setProgress(workDataOf("progress" to (info.mDownloadSize * 1.0 / info.mFileSize)))
            }
            delay(1000)
        }
        return Result.success()
    }
}
