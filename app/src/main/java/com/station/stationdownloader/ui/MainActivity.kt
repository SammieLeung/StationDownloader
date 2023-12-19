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
import com.station.stationdownloader.R
import com.station.stationdownloader.TaskService
import com.station.stationdownloader.TaskStatusServiceImpl
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.navgator.AppNavigator
import com.station.stationdownloader.navgator.Destination
import com.station.stationdownloader.ui.base.BaseActivity
import com.station.stationdownloader.ui.fragment.AddUriDialogFragment
import com.station.stationdownloader.ui.fragment.newtask.AddMultiTaskDialogFragment
import com.station.stationdownloader.ui.fragment.newtask.AddSingleTaskDialogFragment
import com.station.stationdownloader.ui.fragment.newtask.MultiTaskDetailDialogFragment
import com.station.stationdownloader.ui.viewmodel.MainViewModel
import com.station.stationdownloader.ui.viewmodel.ToastAction
import com.station.stationdownloader.ui.viewmodel.ToastState
import com.station.stationdownloader.ui.viewmodel.UiAction
import com.station.stationdownloader.utils.DLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), DLogger {

    val vm: MainViewModel by viewModels()
    var toast: Toast? = null
    var focusView: View? = null


    @Inject
    lateinit var navigator: AppNavigator

    val tabViewList: MutableList<View> = mutableListOf()

    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TaskStatusServiceImpl
            vm.bindDownloadingTaskStatusMap(binder.getService().getDownloadingTaskStatusMap())
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
                        if (it.isShowAddNewTask) {
                            if (supportFragmentManager.findFragmentByTag(
                                    AddSingleTaskDialogFragment::class.java.simpleName
                                )?.isVisible == true
                            ) {
                                return@collect
                            }
                            val dialog = AddSingleTaskDialogFragment()
                            Logger.d("dialog = $dialog")
                            dialog.show(
                                supportFragmentManager,
                                AddSingleTaskDialogFragment::class.java.simpleName
                            )
                        } else if (it.isShowAddMultiTask) {
                            if (supportFragmentManager.findFragmentByTag(
                                    AddMultiTaskDialogFragment::class.java.simpleName
                                )?.isVisible == true
                            ) {
                                return@collect
                            }
                            val dialog = AddMultiTaskDialogFragment()
                            Logger.d("dialog = $dialog")
                            dialog.show(
                                supportFragmentManager,
                                AddMultiTaskDialogFragment::class.java.simpleName
                            )
                        } else if (it.isShowMultiTaskDetail) {
                            if (supportFragmentManager.findFragmentByTag(
                                    MultiTaskDetailDialogFragment::class.java.simpleName
                                )?.isVisible == true) {
                                return@collect
                            }
                            val multiTaskDialog = supportFragmentManager.findFragmentByTag(
                                AddMultiTaskDialogFragment::class.java.simpleName
                            )
                            if (multiTaskDialog?.isVisible == true) {
                                (multiTaskDialog as AddMultiTaskDialogFragment).dismiss()
                            }
                            val dialog=MultiTaskDetailDialogFragment()
                            dialog.show(
                                supportFragmentManager,
                                MultiTaskDetailDialogFragment::class.java.simpleName
                            )
                        }
                    }
                }
                launch {
                    vm.toastState.filter { it is ToastState.Show }
                        .map { it as ToastState.Show }
                        .collect {
                            withContext(Dispatchers.Main) {
                                showToast(applicationContext, it.msg)
                                vm.emitToast(ToastAction.InitToast)
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
        focusView = currentFocus
        onNewIntent(intent)
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleAddUriIntent(intent, vm.accept, vm.emitToast)
    }


    override fun onResume() {
        super.onResume()
        bindService(
            Intent(this, TaskService::class.java),
            serviceConnection,
            Context.BIND_AUTO_CREATE
        )
        requestFocusOnSelectedTab()
    }


    override fun onPause() {
        super.onPause()
        Logger.d("onPause")
        vm.accept(UiAction.SaveSession)
        unbindService(serviceConnection)
    }

    private fun ActivityMainBinding.bindState() {
        addUriBtn.setOnClickListener {
            AddUriDialogFragment().show(supportFragmentManager, "")
        }

    }


    private fun requestFocusOnSelectedTab() {
        tabViewList.forEach {
            if (it.isSelected) {
                it.requestFocus()
                logger(focusView)
            }
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
        view.setOnClickListener { view ->
            tabViewList.forEach { it.isSelected = false }
            view.isSelected = true
            navigator.navigateTo(destination)
        }
        view.onFocusChangeListener =
            OnFocusChangeListener { v, hasFocus -> if (hasFocus) v.performClick() }
    }

    private fun handleAddUriIntent(
        intent: Intent?,
        accept: (UiAction) -> Unit,
        emitToast: (ToastAction) -> Unit
    ) {
        if (intent == null) return
        when (intent.action) {
            ACTION_ADD_URI -> {
                val uri = intent.getStringExtra(Intent.EXTRA_TEXT)
                uri?.let {
                    if (it.isMultiUri()) {
                        val uriList = it.split("\n").filter { it.isNotEmpty() }
                        if (uriList.isEmpty()) {
                            emitToast(ToastAction.ShowToast(getString(R.string.uri_is_empty)))
                        } else if (uriList.size == 1) {
                            accept(UiAction.InitSingleTask(uriList[0]))
                        } else {
                            accept(UiAction.InitMultiTask(uriList))
                        }
                    } else {
                        accept(UiAction.InitSingleTask(uri))
                    }
                } ?: emitToast(ToastAction.ShowToast(getString(R.string.uri_is_empty)))


            }

            else -> {}
        }
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

    companion object {
        const val ACTION_ADD_URI = "com.station.stationdownloader.ADD_URI"

        @JvmStatic
        fun newIntent(welcomeActivity: WelcomeActivity, intent: Intent? = null): Intent? {
            return Intent(welcomeActivity, MainActivity::class.java).apply {
                intent?.let {
                    this.putExtras(it)
                    this.data = it.data
                    this.action = it.action
                    this.type = it.type
                }
            }
        }
    }
}

private fun String.isMultiUri(): Boolean {
    return this.contains("\n")
}