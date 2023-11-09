package com.station.stationdownloader.data.source.remote.json

import com.station.stationdownloader.DownloadTaskStatus
import com.station.stationdownloader.ITaskState
import com.station.stationdownloader.data.IResult
import com.station.stationdownloader.data.source.ITorrentInfoRepository
import com.station.stationdownloader.data.source.local.room.entities.XLDownloadTaskEntity

data class RemoteStartTask(
    val data: RemoteStartTaskData,
    val expand: RemoteTask,
) {
    constructor(
        url: String,
        task_id: String,
        id: Long,
        down_size: Long,
        download_path: String,
        file_count: Int,
        is_multifile: Boolean,
        is_torrent_task: Boolean,
        status: Int,
        task_name: String,
        total_size: Long,
        hash: String = "",
        create_time: Long
    ) : this(
        RemoteStartTaskData(url, task_id),
        RemoteTask(
            id,
            down_size,
            download_path,
            file_count,
            is_multifile,
            is_torrent_task,
            status,
            task_id,
            task_name,
            total_size,
            url,
            hash,
            create_time
        )

    )
    
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

data class RemoteStartTaskData(
    val url: String,
    val task_id: String,
)

