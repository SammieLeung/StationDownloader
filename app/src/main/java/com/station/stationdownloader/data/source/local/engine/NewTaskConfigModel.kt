package com.station.stationdownloader.data.source.local.engine

import com.xunlei.downloadlib.parameter.TorrentInfo

sealed class NewTaskConfigModel {
    data class FileTaskConfig(
        /*用户输入的下载链接*/
        val originUrl:String,
        /*经过系统处理的链接*/
        val url:String,
        val taskName: String,
        val downloadPath: String,
        val fileType: String,
        val fileSize: Long,
        val fileExt: String
    ) : NewTaskConfigModel()

    data class TorrentTaskConfig(
        val taskName: String,
        val downloadPath: String,
        val torrentInfo:TorrentInfo
    ) : NewTaskConfigModel()

}