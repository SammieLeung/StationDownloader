package com.station.stationdownloader.vm

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.datasource.IEngineRepository
import com.station.stationdownloader.data.datasource.ITorrentInfoRepository
import com.station.stationdownloader.data.repository.DefaultEngineRepository
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
//                launch {
//                    val t = System.currentTimeMillis()
//                    val movieResult = engineRepo.initTask("sdcard/Station/torrent/movie.torrent")
//                    if (movieResult is IResult.Success) {
//                        (engineRepo as DefaultEngineRepository).downloadTorrent(movieResult.data)
//                    }
//                    Logger.d("launch 1 ${System.currentTimeMillis() - t} ms")
//
//                }
//                launch {
//                    val t = System.currentTimeMillis()
//
//                    val tvResult = engineRepo.initTask("sdcard/Station/torrent/tvset.torrent")
//                    Logger.d("launch 2 ${System.currentTimeMillis() - t} ms")
//
//                }
//                launch {
//                    val t = System.currentTimeMillis()
//
//                    val tvResult = engineRepo.initTask("magnet:?xt=urn:btih:8dacc0ea996d7e6dabd65044f413d34ad9d27a2e&dn=%e9%98%b3%e5%85%89%e7%94%b5%e5%bd%b1dy.ygdy8.com.%e8%b6%85%e7%ba%a7%e9%a9%ac%e5%8a%9b%e6%ac%a7%e5%85%84%e5%bc%9f%e5%a4%a7%e7%94%b5%e5%bd%b1.2023.BD.1080P.%e4%b8%ad%e8%8b%b1%e5%8f%8c%e5%ad%97.mkv&tr=udp%3a%2f%2ftracker.opentrackr.org%3a1337%2fannounce&tr=udp%3a%2f%2fexodus.desync.com%3a6969%2fannounce")
//                    val data=(tvResult  as IResult.Success).data
//                    Logger.d("$data")
//
//                }
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