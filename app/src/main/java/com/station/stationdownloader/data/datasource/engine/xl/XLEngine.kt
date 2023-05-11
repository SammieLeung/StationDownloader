package com.station.stationdownloader.data.datasource.engine.xl

import android.content.Context
import com.station.stationdownloader.contants.ConfigureError
import com.station.stationdownloader.contants.DOWNLOAD_SPEED_LIMIT
import com.station.stationdownloader.contants.SPEED_LIMIT
import com.station.stationdownloader.contants.UPLOAD_SPEED_LIMIT
import com.station.stationdownloader.data.datasource.engine.ExecuteResult
import com.station.stationdownloader.data.datasource.engine.IEngine
import com.station.stationdownloader.data.datasource.model.StationDownloadTask
import com.xunlei.downloadlib.XLDownloadManager
import com.xunlei.downloadlib.XLTaskHelper

class XLEngine(
    private val context: Context,
) : IEngine {
    private var hasInit = false

    override fun init() {
        if (!hasInit) {
            synchronized(this@XLEngine) {
                if (!hasInit) {
                    hasInit = true
                    XLTaskHelper.init(context)
                }
            }
        }
    }

    override fun unInit() {
        if (hasInit) {
            synchronized(this@XLEngine) {
                if (hasInit) {
                    hasInit = false
                    XLTaskHelper.uninit()
                }
            }
        }
    }

    override fun initTask(url: String): ExecuteResult<StationDownloadTask> {
        TODO()
    }

    override fun startTask(task: StationDownloadTask) {
        TODO("Not yet implemented")
    }

    override fun stopTask(task: StationDownloadTask) {
        TODO("Not yet implemented")
    }

    override fun configure(key: String, values: Array<String>): ExecuteResult<Nothing> {
        try {
            when (key) {
                UPLOAD_SPEED_LIMIT, DOWNLOAD_SPEED_LIMIT, SPEED_LIMIT -> {
                    if (values.size == 2) {
                        val upSpeedLimit: Long = values[0] as Long
                        val downloadSpeedLimit: Long = values[1] as Long
                        XLDownloadManager.getInstance()
                            .setSpeedLimit(upSpeedLimit, downloadSpeedLimit)
                        return ExecuteResult.Success
                    }
                }

                else -> {
                    return ExecuteResult.Failed(
                        ConfigureError.NOT_SUPPORT_CONFIGURATION.ordinal,
                        Exception(ConfigureError.NOT_SUPPORT_CONFIGURATION.name)
                    )
                }
            }
            return ExecuteResult.Failed(
                ConfigureError.INSUFFICIENT_NUMBER_OF_PARAMETERS.ordinal,
                Exception(ConfigureError.INSUFFICIENT_NUMBER_OF_PARAMETERS.name)
            )
        } catch (e: Exception) {
            return ExecuteResult.Failed(
                ConfigureError.CONFIGURE_ERROR.ordinal,
                Exception(e)
            )
        }
    }

}