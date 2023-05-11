package com.station.stationdownloader.ui

import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.lifecycleScope
import com.facebook.stetho.Stetho
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.station.stationdownloader.databinding.ActivityWelcomeBinding
import com.station.stationdownloader.ui.base.PermissionActivity
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class WelcomeActivity : PermissionActivity<ActivityWelcomeBinding>(
    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
) {


    override fun grantAllPermissions() {
        super.grantAllPermissions()
        lifecycleScope.launch {
            startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
            finish()
        }
    }
}
