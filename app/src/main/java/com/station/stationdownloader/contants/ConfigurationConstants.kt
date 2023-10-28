package com.station.stationdownloader.contants

import android.os.Environment
import java.io.File

sealed class Options(val key: String) {
    override fun toString(): String {
        return key
    }
}

sealed class CommonOptions(key: String): Options(key) {
    object MaxThread : CommonOptions("max_thread")
    object DownloadPath : CommonOptions("download_path")
    object DefaultDownloadEngine : CommonOptions("download_engine")
}

sealed class Aria2Options(key: String): Options(key) {
    object SpeedLimit : Aria2Options("aria2_speed_limit")
}

sealed class XLOptions(key: String): Options(key) {
    object SpeedLimit : XLOptions("xl_speed_limit")
}

const val DEFAULT_MAX_CONCURRENT_DOWNLOADS_COUNT = 5
const val DEFAULT_DOWNLOAD_DIR = "Station/Download"


//轮询下载种子任务信息的时间间隔
const val TORRENT_DOWNLOAD_TASK_INTERVAL = 50L

//轮询下载种子任务信息的超时时间
const val TORRENT_DOWNLOAD_TASK_TIMEOUT = 10000L

val tryDownloadDirectoryPath: String by lazy {
    File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ".tryDownload"
    ).path
}
