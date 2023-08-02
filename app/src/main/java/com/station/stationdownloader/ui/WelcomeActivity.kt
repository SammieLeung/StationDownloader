package com.station.stationdownloader.ui

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
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

    @Inject
    @XLEngineAnnotation
    lateinit var mXLEngine: IEngine
    override fun grantAllPermissions() {
        super.grantAllPermissions()
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                Logger.addLogAdapter(AndroidLogAdapter())
                DimenUtils.init(context = applicationContext)
                MoshiHelper.init()
                MimeTypeHelper.init(applicationContext)
                mXLEngine.init()
                startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
                finish()
            }
        }
    }
}
