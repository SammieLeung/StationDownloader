package com.station.stationdownloader.data.source.local

import android.os.Environment
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.contants.DEFAULT_DOWNLOAD_PATH
import com.station.stationdownloader.contants.DOWNLOAD_ENGINE
import com.station.stationdownloader.contants.DOWNLOAD_PATH
import com.station.stationdownloader.contants.MAX_THREAD
import com.station.stationdownloader.contants.MAX_THREAD_COUNT
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.data.source.IConfigurationDataSource
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


class ConfigurationLocalDataSource internal constructor(
    private val defaultMMKV: MMKV,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IConfigurationDataSource {

    override suspend fun getSpeedLimit(): Long = withContext(ioDispatcher){
         defaultMMKV.decodeLong(SPEED_LIMIT, -1)
    }

    override suspend fun setSpeedLimit(speedLimit: Long): Boolean = withContext(ioDispatcher) {
        defaultMMKV.encode(SPEED_LIMIT, speedLimit)
    }

    override suspend fun getDownloadPath(): String  = withContext(ioDispatcher){
         defaultMMKV.decodeString(DOWNLOAD_PATH)
            ?: File(Environment.getExternalStorageDirectory(), DEFAULT_DOWNLOAD_PATH).path
    }

    override suspend fun setDownloadPath(path: String): Boolean = withContext(ioDispatcher) {
        defaultMMKV.encode(DOWNLOAD_PATH, path)
    }

    override suspend fun getMaxThread(): Int = withContext(ioDispatcher) {
         defaultMMKV.decodeInt(MAX_THREAD, MAX_THREAD_COUNT)
    }

    override suspend fun setMaxThread(count: Int): Boolean = withContext(ioDispatcher) {
        defaultMMKV.encode(MAX_THREAD, count)
    }

    override suspend fun setDefaultEngine(engine: DownloadEngine) : Boolean= withContext(ioDispatcher) {
        defaultMMKV.encode(DOWNLOAD_ENGINE, engine.name)
    }

    override suspend fun getDefaultEngine(): DownloadEngine = withContext(ioDispatcher) {
         DownloadEngine.valueOf(
            defaultMMKV.decodeString(DOWNLOAD_ENGINE) ?: DownloadEngine.XL.name
        )
    }
}