package com.station.stationdownloader.data.source.remote.json

/*
    "down_size":897413294,
    "download_path":"storage/emulated/0/Download//",
    "file_count":1,
    "id":1,
    "is_multifile":false,
    "is_torrent_task":false,
    "status":0,
    "task_id":-1,
    "task_name":"阳光电影www.ygdy8.com.女村医.HD.1080p.国语中字.mp4",
    "total_size":1490390866,
    "url":"thunder://QUFmdHA6Ly95Z2R5ODp5Z2R5OEB5ZzE4LmR5ZHl..."
 */
data class RemoteTask(
    val id: Long,
    val down_size: Long,
    val download_path: String,
    val file_count: Int,
    val is_multifile: Boolean,
    val is_torrent_task: Boolean,
    val status: Int,
    val task_id: String ="",
    val task_name:String,
    val total_size:Long,
    val url:String,
    val hash:String="",
    val create_time:Long
)

