package com.station.stationdownloader.ui

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gianlu.aria2lib.Aria2Ui
import com.gianlu.aria2lib.commonutils.Prefs
import com.gianlu.aria2lib.internal.Aria2
import com.orhanobut.logger.Logger
import com.station.pluginscenter.base.BaseActivity
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.engine.aria2.WebSocketClient
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    val vm: MainViewModel by viewModels<MainViewModel>()
    var toast: Toast? = null
    @Inject
    lateinit var mEngineRepo:IEngineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding.btnStart.setOnClickListener {
//            val request= OneTimeWorkRequestBuilder<TestRebootWorker>()
//                .build()
//            WorkManager.getInstance(baseContext).enqueue(request)
            testAria2UI()
           Logger.d("服务已开启")
        }
    }

    private fun showToast(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    fun testAria2UI(){
        val ui= Aria2Ui(applicationContext,object : Aria2Ui.Listener{
            override fun onUpdateLogs(msg: MutableList<Aria2Ui.LogMessage>) {
                for(m in msg){
                    Logger.d("onUpdateLogs:${m.type.name}")
                }
            }

            override fun onMessage(msg: Aria2Ui.LogMessage) {
                Logger.d("onMessage=$msg")
            }

            override fun updateUi(on: Boolean) {
                Logger.d("updateUi=$on")

            }
        })

        lifecycleScope.launch(Dispatchers.Default) {

            mEngineRepo.init()
            mEngineRepo.startTask(
                url = "/sdcard/Station/test.torrent",
                engine = DownloadEngine.ARIA2,
                downloadPath = "/sdcard/Download",
                name = "test",
                urlType = DownloadUrlType.DIRECT,
                fileCount = 1,
                selectIndexes = IntArray(0)
            )
        }

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
