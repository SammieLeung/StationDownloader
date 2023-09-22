package com.station.stationdownloader.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.StationDownloaderApp
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.databinding.ActivityWelcomeBinding
import com.station.stationdownloader.di.XLEngineAnnotation
import com.station.stationdownloader.ui.base.PermissionActivity
import com.station.stationkitkt.DimenUtils
import com.station.stationkitkt.MimeTypeHelper
import com.station.stationkitkt.MoshiHelper
import com.tencent.mmkv.MMKV
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


    override fun grantAllPermissions() {
        super.grantAllPermissions()
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                LocalBroadcastManager.getInstance(this@WelcomeActivity).registerReceiver(object :BroadcastReceiver(){
                    override fun onReceive(context: Context?, intent: Intent?) {
                        startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                        finish()
                    }
                }, IntentFilter("ACTION_INIT"))
                val app = application as StationDownloaderApp
                if (!app.isInitialized()) {
                    app.initAction()
                }

            }
        }
    }

}
