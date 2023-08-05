package com.station.stationdownloader.data.source.remote.json

/**
 *
"file_count":1,
"info_hash":"05859C887CF3DF8F7C6214057DD20E8F687EC1D6",
"is_multi_files":false,
"name":"station_test",
"sub_fileinfo":[
{
"file_index":0,
"file_name":"阳光电影www.ygdy8.com.釜山行2：半岛.HD.1080p.韩语中字.mkv",
"file_size":2157982354,
"is_selected":true
}
],
"url":"/storage/emulated/0/Download/TvRemote/torrent/station_test.torrent"
 */
data class RemoteTorrentInfo(
    val file_count: Int,
    val info_hash: String,
    val is_multi_files: Boolean,
    val name: String,
    val sub_fileinfo: List<RemoteSubFileInfo>,
    val url: String
)

data class RemoteSubFileInfo(
    val file_index: Int,
    val file_name: String,
    val file_size: Long,
    val is_selected: Boolean
)
