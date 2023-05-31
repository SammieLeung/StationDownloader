package com.station.stationdownloader.vm

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.xunlei.downloadlib.XLTaskHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    val application: Application,
    val stateHandle: SavedStateHandle,
) : ViewModel() {
    @Inject
    lateinit var engineRepo: IEngineRepository

    @Inject
    lateinit var torrentInfoRepo: ITorrentInfoRepository
    fun engine() = viewModelScope.launch {
        withContext(Dispatchers.Default) {
            val g = System.currentTimeMillis()
            coroutineScope {
                launch {
                    val t = System.currentTimeMillis()
                    val movieResult = engineRepo.initTask("sdcard/Station/torrent/movie.torrent")
                    Logger.d("launch 1 ${System.currentTimeMillis() - t} ms")

                }
                launch {
                    val t = System.currentTimeMillis()

                    val tvResult = engineRepo.initTask("sdcard/Station/torrent/tvset.torrent")
                    Logger.d("launch 2 ${System.currentTimeMillis() - t} ms")

                }
            }
            Logger.d("withContext ${System.currentTimeMillis() - g} ms")

        }

    }

    fun torrentInfo() {
        viewModelScope.launch {
            val torrentInfo =
                XLTaskHelper.instance().getTorrentInfo("sdcard/Station/torrent/tvset.torrent")
            torrentInfoRepo.saveTorrentInfo(torrentInfo)
        }
    }
}