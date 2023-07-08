package com.station.stationdownloader.data.source.local.engine

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.source.local.model.TreeNode

sealed class NewTaskConfigModel {
    data class FileTaskConfig(
        /*用户输入的下载链接*/
        val originUrl:String,
        /*经过系统处理的链接*/
        val url:String,
        val taskName: String,
        val downloadPath: String,
        val engine:DownloadEngine=DownloadEngine.XL,
        val fileType: String,
        val fileSize: Long,
        val fileExt: String
    ) : NewTaskConfigModel()

    data class TorrentTaskConfig(
        val filePath:String,
        val taskName: String,
        val downloadPath: String,
        val fileCount:Int,
        val engine:DownloadEngine=DownloadEngine.XL,
        val fileStateList:TreeNode
    ) : NewTaskConfigModel()

}