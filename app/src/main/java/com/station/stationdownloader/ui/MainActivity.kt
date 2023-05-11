package com.station.stationdownloader.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.orhanobut.logger.Logger
import com.station.pluginscenter.base.BaseActivity
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.engine.xl.XLEngine
import com.station.stationdownloader.databinding.ActivityMainBinding
import com.station.stationdownloader.di.XLEngineAnnotation
import com.station.stationdownloader.vm.MainViewModel
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    val vm: MainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.engine()
    }
}