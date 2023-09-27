package com.station.stationdownloader.data.source

import com.gianlu.aria2lib.Aria2Ui
import com.gianlu.aria2lib.Aria2Ui.Listener
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.local.engine.IEngine
import com.station.stationdownloader.data.source.local.engine.NewTaskConfigModel
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.room.entities.TorrentFileInfoEntity
import com.station.stationdownloader.data.source.local.room.entities.TorrentInfoEntity
import kotlinx.coroutines.flow.Flow

interface IEngineRepository {
    fun init(): Flow<Pair<DownloadEngine, IResult<String>>>
    suspend fun loadLocalConfigurations(): IResult<Map<String,IResult<String>>>
    suspend fun unInit(): IResult<Unit>
    suspend fun initUrl(url: String): IResult<NewTaskConfigModel>
    suspend fun getTorrentInfo(torrentPath: String): IResult<Map<TorrentInfoEntity, List<TorrentFileInfoEntity>>>
    suspend fun startTask(
        stationDownloadTask: StationDownloadTask
    ): IResult<Long>

    suspend fun stopTask(currentTaskId: Long, stationDownloadTask: StationDownloadTask)
    suspend fun restartTask(
        currentTaskId: Long,
        stationDownloadTask: StationDownloadTask
    ): IResult<Long>

    suspend fun configure(key: String, value: String): IResult<Unit>

    suspend fun getEngineStatus(): Map<DownloadEngine, IEngine.EngineStatus>

    fun addAria2Listener(listener: Aria2Ui.Listener)
    fun removeAria2Listener(listener: Listener)
}

