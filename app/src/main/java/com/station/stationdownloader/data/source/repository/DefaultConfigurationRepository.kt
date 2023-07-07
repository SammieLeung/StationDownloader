package com.station.stationdownloader.data.source.repository

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.contants.DOWNLOAD_SPEED_LIMIT
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.contants.UPLOAD_SPEED_LIMIT
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.station.stationdownloader.data.source.IConfigurationRepository
import com.station.stationdownloader.data.source.local.engine.IEngine

class DefaultConfigurationRepository(
    private val xlEngine: IEngine,
    private val aria2Engine: IEngine,
    private val localDataSource: IConfigurationDataSource
) : IConfigurationRepository {
    override suspend fun getDownloadSpeedLimit(): Long {
        return localDataSource.getDownloadSpeedLimit()
    }

    override suspend fun setDownloadSpeedLimit(downloadSpeedLimit: Long) {
        localDataSource.setDownloadSpeedLimit(downloadSpeedLimit)
        xlEngine.configure(
            DOWNLOAD_SPEED_LIMIT,
            arrayOf(getUploadSpeedLimit().toString(), downloadSpeedLimit.toString())
        )
        aria2Engine.configure(
            DOWNLOAD_SPEED_LIMIT,
            arrayOf(getUploadSpeedLimit().toString(), downloadSpeedLimit.toString())
        )
    }

    override suspend fun getUploadSpeedLimit(): Long {
        return localDataSource.getUploadSpeedLimit()
    }

    override suspend fun setUploadSpeedLimit(uploadSpeedLimit: Long) {
        localDataSource.setUploadSpeedLimit(uploadSpeedLimit)
        xlEngine.configure(
            UPLOAD_SPEED_LIMIT,
            arrayOf(uploadSpeedLimit.toString(), getDownloadSpeedLimit().toString())
        )
        aria2Engine.configure(
            UPLOAD_SPEED_LIMIT,
            arrayOf(uploadSpeedLimit.toString(), getDownloadSpeedLimit().toString())
        )
    }

    override suspend fun getSpeedLimit(): Long {
        return localDataSource.getSpeedLimit()
    }

    override suspend fun setSpeedLimit(speedLimit: Long) {
        localDataSource.setSpeedLimit(speedLimit)
        xlEngine.configure(
            SPEED_LIMIT,
            arrayOf(speedLimit.toString(), speedLimit.toString())
        )
        aria2Engine.configure(
            SPEED_LIMIT,
            arrayOf(speedLimit.toString(), speedLimit.toString())
        )
    }

    override suspend fun getDownloadPath(): String {
        return localDataSource.getDownloadPath()
    }

    override suspend fun setDownloadPath(path: String) {
        localDataSource.setDownloadPath(path)
    }

    override suspend fun getMaxThread(): Int {
        return localDataSource.getMaxThread()
    }

    override suspend fun setMaxThread(count: Int) {
        localDataSource.setMaxThread(count)
    }

    override suspend fun setDefaultEngine(engine: DownloadEngine) {
        localDataSource.setDefaultEngine(engine)
    }

    override suspend fun getDefaultEngine(): DownloadEngine {
        return localDataSource.getDefaultEngine()
    }
}

