package com.station.stationdownloader.data.source.local.engine

import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import com.station.stationdownloader.utils.TaskTools

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
        val urlType: DownloadUrlType,
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
        val torrentId:Long,
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

fun NewTaskConfigModel.asXLDownloadTaskEntity(): XLDownloadTaskEntity {
    return when (this) {
        is NewTaskConfigModel.NormalTask -> {
            val root = this._fileTree as TreeNode.Directory
            val fileSize = root.totalCheckedFileSize
            XLDownloadTaskEntity(
                id = 0,
                torrentId = -1,
                url = this.originUrl,
                name = this.taskName,
                urlType = urlType,
                engine = this.engine,
                totalSize = fileSize,
                downloadPath = this.downloadPath
            )

        }

        is NewTaskConfigModel.TorrentTask -> {
            val root = this._fileTree as TreeNode.Directory
            val fileSize = root.totalCheckedFileSize
            XLDownloadTaskEntity(
                id = 0,
                torrentId = this.torrentId,
                url = this.torrentPath,
                name = this.taskName,
                urlType = DownloadUrlType.TORRENT,
                engine = this.engine,
                totalSize = fileSize,
                downloadPath = this.downloadPath
            )

        }
    }
}