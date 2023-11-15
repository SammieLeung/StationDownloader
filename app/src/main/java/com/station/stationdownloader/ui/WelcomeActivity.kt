package com.station.stationdownloader.ui

import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.R
import com.station.stationdownloader.StationDownloaderApp
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.usecase.EngineRepoUseCase
import com.station.stationdownloader.databinding.ActivityWelcomeBinding
import com.station.stationdownloader.ui.base.PermissionActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@AndroidEntryPoint
class WelcomeActivity : PermissionActivity<ActivityWelcomeBinding>(
    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
) {
    @Inject
    lateinit var engineRepoUseCase: EngineRepoUseCase


    override fun grantAllPermissions() {
        super.grantAllPermissions()

        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val app = application as StationDownloaderApp
                val messageList= mutableListOf<String>()
                if (!app.isInitialized()) {
                    app.initialize()
                }

                if(!engineRepoUseCase.isEnginesInitialized()){
                    engineRepoUseCase.initEngines { engine, result ->
                        when (engine) {
                            DownloadEngine.XL -> {
                                if(result is IResult.Success) {
                                    messageList.add(getString(R.string.defalut_engine_initailize_succeed))
                                } else if (result is IResult.Error) {
                                    messageList.add(getString(R.string.defalut_engine_initailize_error))
                                    Logger.e("[XL] init error: ${result.exception}")
                                }
                                mBinding.process= messageList.joinToString("\n")
                            }
                            DownloadEngine.ARIA2 -> {
                                if(result is IResult.Success) {
                                    messageList.add(getString(R.string.aria2_engine_initailize_succeed))
                                } else if (result is IResult.Error) {
                                    messageList.add(getString(R.string.aria2_engine_initailize_error))
                                    Logger.e("[Aria2] init error: ${result.exception}")
                                }
                                messageList.add(getString(R.string.welcome_enter_message))
                                mBinding.process= messageList.joinToString("\n")
                                delay(1000)
                                startActivity(MainActivity.newIntent(this@WelcomeActivity))
                                finish()
                            }

                            DownloadEngine.INVALID_ENGINE -> {}
                        }
                    }
                }
                else{
                    startActivity(MainActivity.newIntent(this@WelcomeActivity))
                    finish()
                }

            }
        }
    }


}
