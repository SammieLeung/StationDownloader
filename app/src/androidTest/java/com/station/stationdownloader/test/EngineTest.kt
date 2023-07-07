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
        Logger.addLogAdapter(AndroidLogAdapter())
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        runBlocking { mEngineRepo.init() }
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
                   val magnetTask="magnet:?xt=urn:btih:c9997e77250a42e2ca912d1842e1727a34fbc295&dn=%5Bmp4kan%5Dd%E8%88%8Cl%E5%B8%88.2023.HD1080p.%E7%B2%A4%E8%AF%AD%E4%B8%AD%E5%AD%97.v2.mp4"
                   val thunderTask="thunder://QUFodHRwczovL3JlZGlyZWN0b3IuZ3Z0MS5jb20vZWRnZWRsL2FuZHJvaWQvc3R1ZGlvL2lkZS16aXBzLzIwMjIuMi4xLjIwL2FuZHJvaWQtc3R1ZGlvLTIwMjIuMi4xLjIwLWxpbnV4LnRhci5nelpa"
                    val httpTask="https://vt1.doubanio.com/202307071536/b60b7e471c15b36d2fa3db6104bbeb3a/view/movie/M/403060358.mp4"
                    val taskflow =
                        flow {
                            val result = mEngineRepo
                                .initTask(httpTask)
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
                        }

                    taskflow.collect{
                        Logger.d(TaskTools.toHumanReading(it.totalSize))
                        Logger.d(it)
                    }

                    val startTask = taskflow.map {
                        val result = mEngineRepo.startTask(
                            it.url,
                            it.engine,
                            it.downloadPath,
                            it.name,
                            it.urlType,
                            it.fileCount,
                            it.selectIndexes.toIntArray()
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
                    }


//                    startTask.collect {
//                        Logger.d("start taskId $it")
//
//                        val request = OneTimeWorkRequestBuilder<StatusWorker>()
//                            .setInputData(workDataOf("taskId" to it))
//                            .build()
//                        val workManager = WorkManager.getInstance(context)
//
//                        workManager.enqueue(request)
//                        val workInfo = workManager.getWorkInfoById(request.id).get()
//                        delay(3000)
//                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
//                        delay(3000)
//                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
//                        delay(3000)
//                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
//                        WorkManager.getInstance(context).cancelAllWorkByTag("runningTask")
//                        delay(3000)
//                        Logger.d("workInfo ${workInfo.state}")
//                        Logger.d("workInfo ${workInfo.progress.getDouble("progress", 0.0)}")
//                        delay(3000)
//                        Logger.d("workInfo ${workInfo.state}")
//                        delay(100000)
//
//                    }

                }
            }

        }
            Logger.i("runBlocking")
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
