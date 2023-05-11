package com.station.stationdownloader.data.datasource.local

import android.os.Environment
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.contants.DOWNLOAD_ENGINE
import com.station.stationdownloader.contants.DOWNLOAD_PATH
import com.station.stationdownloader.contants.DOWNLOAD_SPEED_LIMIT
import com.station.stationdownloader.contants.MAX_THREAD
import com.station.stationdownloader.contants.MAX_THREAD_COUNT
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.contants.UPLOAD_SPEED_LIMIT
import com.station.stationdownloader.data.datasource.IConfigurationDataSource
import com.tencent.mmkv.MMKV
import com.xunlei.downloadlib.XLDownloadManager


class ConfigurationLocalDataSource(
    private val defaultMMKV: MMKV,
) : IConfigurationDataSource {

    override fun getDownloadSpeedLimit(): Long {
        return defaultMMKV.decodeString(DOWNLOAD_SPEED_LIMIT, "-1")?.toLong() ?: -1L
    }

    override fun setDownloadSpeedLimit(downloadSpeedLimit: Long) {
        defaultMMKV.encode(DOWNLOAD_SPEED_LIMIT, downloadSpeedLimit)
    }

    override fun getUploadSpeedLimit(): Long {
        return defaultMMKV.decodeString(UPLOAD_SPEED_LIMIT, "-1")?.toLong() ?: -1L
    }

    override fun setUploadSpeedLimit(uploadSpeedLimit: Long) {
        defaultMMKV.encode(UPLOAD_SPEED_LIMIT, uploadSpeedLimit)
    }

    override fun getSpeedLimit(): Long {
        return defaultMMKV.decodeString(SPEED_LIMIT, "-1")?.toLong() ?: -1L
    }

    override fun setSpeedLimit(speedLimit: Long) {
        defaultMMKV.encode(SPEED_LIMIT, speedLimit)
    }

    override fun getDownloadPath(): String {
        return defaultMMKV.decodeString(DOWNLOAD_PATH)
            ?: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path
    }

    override fun setDownloadPath(path: String) {
        defaultMMKV.encode(DOWNLOAD_PATH, path)
    }

    override fun getMaxThread(): Int {
        return defaultMMKV.decodeInt(MAX_THREAD, MAX_THREAD_COUNT)
    }

    override fun setMaxThread(count: Int) {
        defaultMMKV.encode(MAX_THREAD, count)
    }

    override fun setDefaultEngine(engine: DownloadEngine) {
        defaultMMKV.encode(DOWNLOAD_ENGINE, engine.name)
    }

    override fun getDefaultEngine(): DownloadEngine {
        return DownloadEngine.valueOf(
            defaultMMKV.decodeString(DOWNLOAD_ENGINE) ?: DownloadEngine.XL.name
        )
    }
}