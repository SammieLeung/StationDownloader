package com.station.stationdownloader.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnFocusChangeListener
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gianlu.aria2lib.Aria2Ui
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.navgator.AppNavigator
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseActivity
import com.station.stationdownloader.ui.fragment.DownloadedTaskFragment
import com.station.stationdownloader.ui.fragment.DownloadingTaskFragment
import com.station.stationdownloader.ui.fragment.SettingsFragment
import com.station.stationdownloader.vm.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    val vm: MainViewModel by viewModels<MainViewModel>()
    var toast: Toast? = null

    val fragment1: DownloadingTaskFragment by lazy {
        DownloadingTaskFragment()
    }
    val fragment2: DownloadedTaskFragment by lazy {
        DownloadedTaskFragment()
    }
    val fragment3: SettingsFragment by lazy {
        SettingsFragment()
    }


    @Inject
    lateinit var mEngineRepo: IEngineRepository

    @Inject
    lateinit var navigator: AppNavigator
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTabLayout()
    }

    private fun initTabLayout() {
        navigator.navigateTo(Destination.DOWNLOADING)
        initTabAction(mBinding.downloadedTaskItem, Destination.DOWNLOADED)
        initTabAction(mBinding.downloadingTaskItem, Destination.DOWNLOADING)
        initTabAction(mBinding.settingItem, Destination.SETTINGS)
    }

    private fun initTabAction(view: View, destination: Destination) {
        view.setOnClickListener {
            navigator.navigateTo(destination)
        }
        view.onFocusChangeListener =
            OnFocusChangeListener { v, hasFocus -> if (hasFocus) v.performClick() }
    }


    private fun showToast(context: Context, message: String) {
        toast?.cancel()
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast?.show()
    }

    fun testAria2UI() {

        lifecycleScope.launch(Dispatchers.Default) {

            mEngineRepo.init()
            mEngineRepo.startTask(
                url = "/sdcard/Station/test.torrent",
                engine = DownloadEngine.ARIA2,
                downloadPath = "/sdcard/Download",
                name = "test",
                urlType = DownloadUrlType.TORRENT,
                fileCount = 1,
                selectIndexes = IntArray(0)
            )
        }

    }


    class TestRebootWorker(context: Context, workerParameters: WorkerParameters) :
        CoroutineWorker(context, workerParameters) {
        override suspend fun doWork(): Result {
            var count = 0
            while (count < 60) {
                Logger.d("TestRebootWorker $count")
                count++
                delay(1000)
            }
            return Result.success()
        }
    }
}
