package com.station.stationdownloader.contants

import android.os.Environment
import java.io.File

const val DOWNLOAD_PATH = "download_path"
const val SPEED_LIMIT = "speed_limit"
const val MAX_THREAD = "max_thread"
const val DOWNLOAD_ENGINE = "download_engine"

const val MAX_THREAD_COUNT = 5

const val DEFAULT_DOWNLOAD_PATH = "Station/Download"

//磁链下载种子任务
const val GET_MAGNET_TASK_INFO_DELAY = 1L
const val MAGNET_TASK_TIMEOUT = 5000L

val tryDownloadDirectoryPath: String by lazy {
    File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        ".tryDownload"
    ).path
}
