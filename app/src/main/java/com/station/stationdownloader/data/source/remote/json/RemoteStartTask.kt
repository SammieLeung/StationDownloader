package com.station.stationdownloader.data.source.remote.json

import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity

data class RemoteStartTask(
    val url: String,
    val task_id: String,
    val id: Long,
    val down_size: Long,
    val download_path: String,
    val file_count: Int,
    val is_multifile: Boolean,
    val is_torrent_task: Boolean,
    val status: Int,
    val task_name:String,
    val total_size:Long,
    val hash:String="",
    val create_time:Long
) {
    companion object{
        @JvmStatic
        suspend fun Create(entity:XLDownloadTaskEntity, taskId: String, torrentRepo:ITorrentInfoRepository):RemoteStartTask{
            return RemoteStartTask(
                url = entity.url,
                task_id = taskId,
                id = entity.id,
                down_size = entity.downloadSize,
                download_path = entity.downloadPath,
                file_count = entity.fileCount,
                is_multifile = entity.fileCount > 1,
                is_torrent_task = entity.torrentId > 0,
                status = when (entity.status) {
                    DownloadTaskStatus.DOWNLOADING -> {
                        ITaskState.RUNNING.code
                    }

                    DownloadTaskStatus.COMPLETED -> {
                        ITaskState.DONE.code
                    }

                    else -> {
                        ITaskState.STOP.code
                    }
                },
                task_name = entity.name,
                total_size = entity.totalSize,
                hash = entity.torrentId.takeIf { it > 0 }?.let {
                    torrentRepo.getTorrentHash(it)
                        .takeIf { it is IResult.Success }?.let {
                            (it as IResult.Success).data
                        } ?: ""
                } ?: "",
                create_time = entity.createTime

            )  
        }
    }
}

