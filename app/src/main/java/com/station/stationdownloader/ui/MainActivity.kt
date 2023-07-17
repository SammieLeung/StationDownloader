package com.station.stationdownloader.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.R
import com.station.stationdownloader.contants.EXTRA_CONFIRM_DIALOG
import com.station.stationdownloader.contants.EXTRA_SELECT_TYPE
import com.station.stationdownloader.contants.EXTRA_SUPPORT_NET
import com.station.stationdownloader.contants.EXTRA_TITLE
import com.station.stationdownloader.contants.SelectType
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.navgator.AppNavigator
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseActivity
import com.station.stationdownloader.ui.fragment.newtask.AddNewTaskDialogFragment
import com.station.stationdownloader.ui.fragment.AddUriDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), DLogger {

    val vm: MainViewModel by viewModels<MainViewModel>()
    var toast: Toast? = null
    private val mFilePickerActivityResultContract = SelectFileActivityResultContract()
    private val mFilePickerActivityLauncher =
        registerForActivityResult(mFilePickerActivityResultContract) {
            if (it == Activity.RESULT_OK) {

            }
        }


    @Inject
    lateinit var mEngineRepo: IEngineRepository

    @Inject
    lateinit var navigator: AppNavigator

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    vm.mainUiState.map { it.isLoading }.distinctUntilChanged().collectLatest {
                        mBinding.isLoading = it
                    }
                }
                launch {
                    vm.newTaskState.map { it is NewTaskState.PreparingData }.distinctUntilChanged()
                        .collectLatest {
                            if (it) {
                                if (supportFragmentManager.findFragmentByTag(
                                        AddNewTaskDialogFragment::class.java.simpleName
                                    )?.isVisible == true
                                ) {
                                    return@collectLatest
                                }
                                AddNewTaskDialogFragment().show(
                                    supportFragmentManager,
                                    AddNewTaskDialogFragment::class.java.simpleName
                                )
                            }
                        }

                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTabLayout()
        mBinding.bindState()
    }

    private fun ActivityMainBinding.bindState() {
        addUriBtn.setOnClickListener {
            AddUriDialogFragment().show(supportFragmentManager, "")
        }

        lifecycleScope.launch {

        }

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

    inner class SelectFileActivityResultContract : ActivityResultContract<Unit?, Int>() {
        override fun createIntent(context: Context, input: Unit?): Intent {
            val intent = Intent(ACTION_FILE_PICKER)
            intent.putExtra(EXTRA_SELECT_TYPE, SelectType.SELECT_TYPE_DEVICE.type)
            intent.putExtra(EXTRA_SUPPORT_NET, false)
            intent.putExtra(
                EXTRA_TITLE,
                resources.getString(R.string.title_select_device)
            )
            intent.putExtra(EXTRA_CONFIRM_DIALOG, false)
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Int {
            return resultCode
        }
    }

    companion object {
        const val ACTION_FILE_PICKER = "com.firefly.FILE_PICKER"
    }

    override fun DLogger.tag(): String {
        return MainActivity.javaClass.simpleName
    }
}
