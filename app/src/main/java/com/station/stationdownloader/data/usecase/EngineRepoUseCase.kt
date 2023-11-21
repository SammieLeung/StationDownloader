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
    inline fun initEngines(crossinline block: suspend ((DownloadEngine, IResult<String>) -> Unit)) {
        scope.launch {
            engineRepo.initEngines()
                .collect {
                    block(it.first, it.second)
                }
        }
    }

    inline fun initEngine(engine: DownloadEngine,crossinline  block: suspend (DownloadEngine, IResult<String>) -> Unit) {
        scope.launch {
            engineRepo.initEngine(engine)
                .collect {
                    block(it.first, it.second)
                }
        }
    }

    suspend fun isEnginesInitialized(): Boolean {
        return engineRepo.isEnginesInitialized()
    }

    suspend fun isEngineInitialized(engine: DownloadEngine): Boolean {
        return engineRepo.isEngineInitialized(engine)
    }

    fun initEngines() {
        scope.launch {
            engineRepo.initEngines().collect()
        }
    }

    fun unInitEngines() = scope.launch {
        engineRepo.unInitEngines()
    }

}