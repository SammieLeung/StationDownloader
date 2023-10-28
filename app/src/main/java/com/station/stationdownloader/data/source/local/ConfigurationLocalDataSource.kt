package com.station.stationdownloader.data.source.local

import android.os.Environment
import com.orhanobut.logger.Logger
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.contants.Aria2Options
import com.station.stationdownloader.contants.CommonOptions
import com.station.stationdownloader.contants.DEFAULT_DOWNLOAD_DIR
import com.station.stationdownloader.contants.DEFAULT_MAX_CONCURRENT_DOWNLOADS_COUNT
import com.station.stationdownloader.contants.Options
import com.station.stationdownloader.contants.XLOptions
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
    override suspend fun getValue(key: Options): String = withContext(ioDispatcher) {
        when (key) {
            is CommonOptions -> {
                getValue(key)
            }

            is Aria2Options -> {
                getValue(key)
            }

            is XLOptions -> {
                getValue(key)
            }
        }
    }


    override suspend fun setValue(key: Options, value: String): Boolean =
        withContext(ioDispatcher) {
                defaultMMKV.encode(key.key, value)
        }

    private fun getValue(option: CommonOptions): String {
        return when (option) {
            CommonOptions.MaxThread -> {
                defaultMMKV.decodeString(option.key)
                    ?: DEFAULT_MAX_CONCURRENT_DOWNLOADS_COUNT.toString()
            }

            CommonOptions.DownloadPath -> {
                defaultMMKV.decodeString(option.key)
                    ?: File(Environment.getExternalStorageDirectory(), DEFAULT_DOWNLOAD_DIR).path
            }

            CommonOptions.DefaultDownloadEngine -> {
                defaultMMKV.decodeString(
                    option.key
                ) ?: DownloadEngine.XL.name
            }
        }
    }


    private fun getValue(option: Aria2Options): String {
        return when (option) {
            Aria2Options.SpeedLimit -> defaultMMKV.decodeString(option.key) ?: "0"
        }
    }

    private fun getValue(option: XLOptions): String {
        return when (option) {
            XLOptions.SpeedLimit -> defaultMMKV.decodeString(option.key) ?: "0"
        }
    }
}