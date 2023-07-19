package com.station.stationdownloader.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.Toast
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
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.navgator.AppNavigator
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseActivity
import com.station.stationdownloader.ui.contract.SelectFileActivityResultContract
import com.station.stationdownloader.ui.fragment.AddUriDialogFragment
import com.station.stationdownloader.ui.fragment.newtask.AddNewTaskDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.NewTaskState
import com.station.stationdownloader.ui.viewmodel.ToastState
import com.station.stationdownloader.ui.viewmodel.UiAction
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), DLogger {

    val vm: MainViewModel by viewModels<MainViewModel>()
    var toast: Toast? = null
    private val mFilePickerActivityLauncher =
        registerForActivityResult(SelectFileActivityResultContract()) {
        }


    @Inject
    lateinit var mEngineRepo: IEngineRepository

    @Inject
    lateinit var navigator: AppNavigator

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    vm.mainUiState.map { it.isLoading }.distinctUntilChanged().collectLatest {
                        mBinding.isLoading = it
                    }
                }
                launch {
                    vm.mainUiState.filter { it.toastState is ToastState.Toast }
                        .map { it.toastState as ToastState.Toast }.distinctUntilChanged()
                        .collectLatest {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, it.msg, Toast.LENGTH_SHORT)
                                    .show()
                                vm.accept(UiAction.ResetToast)
                            }
                        }
                }

                launch {
                    vm.newTaskState.filter { it is NewTaskState.PreparingData }
                        .distinctUntilChanged()
                        .collectLatest {
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


    override fun DLogger.tag(): String {
        return MainActivity::class.java.simpleName
    }
}
