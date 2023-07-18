package com.station.stationdownloader

import com.xunlei.downloadlib.XLTaskHelper
import java.io.Serializable

/**
 * author: Sam Leung
 * date:  2023/5/10
 */

/**
 * PENDING->IN_PROGRESS->COMPLETED
 */
enum class DownloadTaskStatus:Serializable {
    PENDING,//准备中
    IN_PROGRESS,//下载中
    COMPLETED,//下载完成
    FAILED//下载失败
}

enum class DownloadUrlType:Serializable  {
    NORMAL,
    THUNDER,
    MAGNET,
    TORRENT,
    HTTP,
    ED2k,
    DIRECT,
    UNKNOWN
}

enum class DownloadEngine:Serializable  {
    XL,
    ARIA2
}

enum class FileType:Serializable{
    VIDEO,
    AUDIO,
    IMG,
    OTHER
}

enum class ITaskState(val code: Int) {
    STOP(0),//停止状态
    RUNNING(1),//下载状态
    DONE(2),//下载完成
    ERROR(-1),//任务错误状态
    FAILED(3),//任务失败
    UNKNOWN(-2)
}

