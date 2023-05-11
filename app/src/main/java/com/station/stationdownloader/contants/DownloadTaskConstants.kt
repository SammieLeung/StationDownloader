package com.station.stationdownloader

/**
 * author: Sam Leung
 * date:  2023/5/10
 */

/**
 * PENDING->IN_PROGRESS->COMPLETED
 */
enum class DownloadTaskStatus {
    PENDING,//准备中
    IN_PROGRESS,//下载中
    COMPLETED,//下载完成
    FAILED//下载失败
}

enum class DownloadUrlType {
    THUNDER,
    META_LINK,
    TORRENT,
    HTTP,
    ED2k,
    DIRECT,
    UNKNOWN
}

enum class DownloadEngine {
    XL,
    ARIA2
}


