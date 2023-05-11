package com.station.stationdownloader.vm

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.di.XLEngineAnnotation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle
) : ViewModel() {
    @Inject
    lateinit var engineRepo: IEngineRepository
    fun engine() {
        val movieResult = engineRepo.initTask("sdcard/Station/torrent/movie.torrent")
        val tvResult = engineRepo.initTask("sdcard/Station/torrent/tvset.torrent")
        val errorResult = engineRepo.initTask("sdcard/Station/torrent/error.torrent")
    }
}