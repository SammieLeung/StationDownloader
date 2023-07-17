package com.station.stationdownloader.data.source.local.engine

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.data.source.local.model.TreeNode

sealed class NewTaskConfigModel(
    val _name: String,
    val _downloadPath: String,
    val _downloadEngine: DownloadEngine,
    val _fileTree: TreeNode
) {
    data class NormalTask(
        /*用户输入的下载链接*/
        val originUrl: String,
        /*经过系统处理的链接*/
        val url: String,
        val taskName: String,
        val downloadPath: String,
        val engine: DownloadEngine = DownloadEngine.XL,
        val fileTree: TreeNode
    ) : NewTaskConfigModel(
        _name = taskName,
        _downloadPath = downloadPath,
        _downloadEngine = engine,
        _fileTree = fileTree
    )

    data class TorrentTask(
        val torrentPath: String,
        val taskName: String,
        val downloadPath: String,
        val fileCount: Int,
        val engine: DownloadEngine = DownloadEngine.XL,
        val fileTree: TreeNode
    ) : NewTaskConfigModel(
        _name = taskName,
        _downloadPath = downloadPath,
        _downloadEngine = engine,
        _fileTree = fileTree
    )

    fun update(
        name: String = this._name,
        downloadPath: String = this._downloadPath,
        downloadEngine: DownloadEngine = this._downloadEngine,
    ): NewTaskConfigModel {
        return when (this) {
            is NormalTask -> {
                this.copy(
                    taskName = name,
                    downloadPath = downloadPath,
                    engine = downloadEngine
                )
            }

            is TorrentTask -> {
                this.copy(
                    taskName = name,
                    downloadPath = downloadPath,
                    engine = downloadEngine
                )
            }
        }
    }

}