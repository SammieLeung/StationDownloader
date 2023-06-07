package com.station.stationdownloader.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.orhanobut.logger.Logger
import com.station.pluginscenter.base.BaseActivity
import com.station.stationdownloader.DownloaderService
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.engine.xl.XLEngine
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.di.XLEngineAnnotation
import com.station.stationdownloader.vm.MainViewModel
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    val vm: MainViewModel by viewModels<MainViewModel>()
    var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.btnStart.setOnClickListener {
            val request= OneTimeWorkRequestBuilder<TestRebootWorker>()
                .build()
            WorkManager.getInstance(baseContext).enqueue(request)
            showToast(baseContext, "服务已开启")
        }
    }

    private fun showToast(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    class TestRebootWorker(context: Context,workerParameters: WorkerParameters): CoroutineWorker(context,workerParameters){
        override suspend fun doWork(): Result {
            var count=0
            while (count<60){
                Logger.d("TestRebootWorker $count")
                count++
                delay(1000)
            }
            return Result.success()
        }
    }
}
