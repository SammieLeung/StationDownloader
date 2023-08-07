package com.station.stationdownloader.data.source.local.engine

import android.provider.ContactsContract.Directory
import com.station.stationdownloader.DownloadEngine
import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.data.source.local.model.StationDownloadTask
import com.station.stationdownloader.data.source.local.model.TreeNode
import com.station.stationdownloader.data.source.local.model.getSelectedFileIndexes
import com.station.stationdownloader.data.source.local.model.setSelectFileIndexes
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity
import java.io.File

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
    ) {
    }

    data class TorrentTask(
        val torrentId: Long,
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
        fileTree: TreeNode = this._fileTree
    ): NewTaskConfigModel {
        return when (this) {
            is NormalTask -> {
                NormalTask(
                    originUrl = originUrl,
                    url = url,
                    taskName = name,
                    urlType = urlType,
                    downloadPath = downloadPath,
                    engine = downloadEngine,
                    fileTree = fileTree,
                )
            }

            is TorrentTask -> {
                TorrentTask(
                    torrentId = torrentId,
                    torrentPath = torrentPath,
                    taskName = name,
                    downloadPath = downloadPath,
                    fileCount = fileCount,
                    engine = downloadEngine,
                    fileTree = fileTree,
                )
            }
        }
    }

    fun updateSelectIndexes(selectIndexes: List<Int>) {
        _fileTree as TreeNode.Directory
        _fileTree.setSelectFileIndexes(selectIndexes)
    }

}

fun NewTaskConfigModel.asStationDownloadTask(): StationDownloadTask {
    return when (this) {
        is NewTaskConfigModel.NormalTask -> {
            val root = this._fileTree as TreeNode.Directory
            val fileSize = root.totalCheckedFileSize
            val selectIndexes = root.getSelectedFileIndexes()
            StationDownloadTask(
                url = this.originUrl,
                realUrl = this.url,
                name = this.taskName,
                urlType = urlType,
                engine = this.engine,
                totalSize = fileSize,
                downloadPath = File(this.downloadPath, taskName).path,
                selectIndexes = selectIndexes,
            )

        }

        is NewTaskConfigModel.TorrentTask -> {
            val root = this._fileTree as TreeNode.Directory
            val fileSize = root.totalCheckedFileSize
            val selectIndexes = root.getSelectedFileIndexes()
            StationDownloadTask(
                torrentId = this.torrentId,
                url = this.torrentPath,
                realUrl = this.torrentPath,
                name = this.taskName,
                urlType = DownloadUrlType.TORRENT,
                engine = this.engine,
                totalSize = fileSize,
                downloadPath = File(this.downloadPath, taskName).path,
                selectIndexes = selectIndexes,
            )

        }
    }
}

fun NewTaskConfigModel.asXLDownloadTaskEntity(): XLDownloadTaskEntity {
    return when (this) {
        is NewTaskConfigModel.NormalTask -> {
            val root = this._fileTree as TreeNode.Directory
            val fileSize = root.totalCheckedFileSize
            val fileCount = root.totalFileCount
            val selectIndexes = root.getSelectedFileIndexes()
            XLDownloadTaskEntity(
                id = 0L,
                url = this.originUrl,
                realUrl = this.url,
                name = this.taskName,
                urlType = urlType,
                engine = this.engine,
                totalSize = fileSize,
                downloadPath = File(this.downloadPath, taskName).path,
                selectIndexes = selectIndexes,
                fileCount = fileCount
            )

        }

        is NewTaskConfigModel.TorrentTask -> {
            val root = this._fileTree as TreeNode.Directory
            val fileSize = root.totalCheckedFileSize
            val fileCount = root.totalFileCount
            val selectIndexes = root.getSelectedFileIndexes()
            XLDownloadTaskEntity(
                id = 0L,
                torrentId = this.torrentId,
                url = this.torrentPath,
                realUrl = this.torrentPath,
                name = this.taskName,
                urlType = DownloadUrlType.TORRENT,
                engine = this.engine,
                totalSize = fileSize,
                downloadPath = File(this.downloadPath, taskName).path,
                selectIndexes = selectIndexes,
                fileCount = fileCount
            )

        }
    }
}

