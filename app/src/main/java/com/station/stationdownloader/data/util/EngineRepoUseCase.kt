package com.station.stationdownloader.data.util

import com.gianlu.aria2lib.Aria2Ui
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import com.station.stationdownloader.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class EngineRepoUseCase constructor(
    val engineRepo: IEngineRepository,
    val scope: CoroutineScope
) {
    inline fun initEngine(crossinline block: suspend ((DownloadEngine, IResult<String>) -> Unit)) {
        scope.launch {
            engineRepo.init()
                .collect {
                    block(it.first, it.second)
                }
        }
    }

    fun initEngine() {
        scope.launch {
            engineRepo.init().collect()
        }
    }

    fun unInitEngine() = scope.launch {
        engineRepo.unInit()
    }

    fun addAria2UiListener(listener: Aria2Ui.Listener) {
        engineRepo.addAria2Listener(listener)
    }

    fun removeAria2UiListener(listener: Aria2Ui.Listener) {
        engineRepo.removeAria2Listener(listener)
    }
}