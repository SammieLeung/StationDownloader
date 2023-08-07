package com.station.stationdownloader.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
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
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskStatusServiceImpl
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.navgator.AppNavigator
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseActivity
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

    val vm: MainViewModel by viewModels()
    var toast: Toast? = null


    @Inject
    lateinit var navigator: AppNavigator

    val tabViewList: MutableList<View> = mutableListOf()

    val serviceConnection=object :ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder= service as TaskStatusServiceImpl
            vm.taskService=binder.getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    vm.mainUiState.collect {
                        mBinding.isLoading = it.isLoading
                        if(it.isShowAddNewTask){
                            if (supportFragmentManager.findFragmentByTag(
                                    AddNewTaskDialogFragment::class.java.simpleName
                                )?.isVisible == true
                            ) {
                                return@collect
                            }
                            val dialog = AddNewTaskDialogFragment()
                            Logger.d("dialog = $dialog")
                            dialog.show(
                                supportFragmentManager,
                                AddNewTaskDialogFragment::class.java.simpleName
                            )
                        }
                    }
                }
                launch {
                    vm.toastState.filter { it is ToastState.Toast }
                        .map { it as ToastState.Toast }
                        .collect {
                            withContext(Dispatchers.Main) {
                                showToast(applicationContext, it.msg)
                                vm.accept(UiAction.ResetToast)
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

    override fun onResume() {
        super.onResume()
        bindService(Intent(this, TaskService::class.java),serviceConnection,Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        unbindService(serviceConnection)
    }

    private fun ActivityMainBinding.bindState() {
        addUriBtn.setOnClickListener {
            AddUriDialogFragment().show(supportFragmentManager, "")
        }

    }

    private fun initTabLayout() {
        navigator.navigateTo(Destination.DOWNLOADING)
        tabViewList.clear()
        tabViewList.addAll(
            listOf(
                mBinding.downloadedTaskItem,
                mBinding.downloadingTaskItem,
                mBinding.settingItem
            )
        )
        initTabAction(mBinding.downloadedTaskItem, Destination.DOWNLOADED)
        initTabAction(mBinding.downloadingTaskItem, Destination.DOWNLOADING)
        initTabAction(mBinding.settingItem, Destination.SETTINGS)
    }

    private fun initTabAction(view: View, destination: Destination) {
        view.setOnClickListener {
            view->
            tabViewList.forEach { it.isSelected=false }
            view.isSelected=true
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
