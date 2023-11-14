package com.station.stationdownloader.contants

import android.os.Environment
import com.gianlu.aria2lib.internal.Aria2
import java.io.File

sealed class Options(val key: String) {
    override fun toString(): String {
        return key
    }
}

sealed class CommonOptions(key: String) : Options(key) {
    object MaxThread : CommonOptions("max_thread")
    object DownloadPath : CommonOptions("download_path")
    object DefaultDownloadEngine : CommonOptions("download_engine")
}

sealed class Aria2Options(key: String) : Options(key) {
    object SpeedLimit : Aria2Options("aria2_speed_limit")
    object BtTracker : Aria2Options("aria2_bt_tracker")
    object BtTrackerLastUpdate : Aria2Options("aria2_bt_tracker_last_update")
}

sealed class XLOptions(key: String) : Options(key) {
    object SpeedLimit : XLOptions("xl_speed_limit")
}

const val BT_TRACKER_UPDATE_INTERVAL = 24 * 60 * 60 * 1000L
const val BT_TRACKER_ARIA2_URL = "https://cf.trackerslist.com/all_aria2.txt"

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
