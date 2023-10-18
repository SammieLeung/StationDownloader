package com.station.stationdownloader.data.usecase

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.repository.DefaultEngineRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class EngineRepoUseCase constructor(
    val engineRepo: DefaultEngineRepository,
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

}