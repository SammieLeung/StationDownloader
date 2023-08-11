package com.station.stationdownloader.data.source.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.station.stationdownloader.data.source.remote.json.RemoteSubFileInfo
import com.station.stationdownloader.data.source.remote.json.RemoteTorrentInfo
import com.station.stationdownloader.utils.TaskTools
import com.xunlei.downloadlib.parameter.TorrentInfo
import java.io.File

@Entity(
    tableName = "torrent_info",
    indices = [Index(value = ["hash","torrent_path"], unique = true)]
)
data class TorrentInfoEntity @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    @ColumnInfo(name = "file_count", defaultValue = "0")
    var fileCount: Int = 0,
    var hash: String,
    @ColumnInfo(name = "torrent_path")
    var torrentPath:String,
    @ColumnInfo(name = "is_multi_files")
    var isMultiFiles: Boolean = false,
    @ColumnInfo(name = "multi_file_base_folder")
    var multiFileBaseFolder: String = "",
)

fun TorrentInfo.asTorrentInfoEntity(torrentPath:String): TorrentInfoEntity {
    return TorrentInfoEntity(
        id = 0,
        fileCount = this.mFileCount,
        hash = this.mInfoHash,
        torrentPath = torrentPath,
        isMultiFiles = this.mIsMultiFiles,
        multiFileBaseFolder = this.mMultiFileBaseFolder
    )
}

fun TorrentInfoEntity.asRemoteTorrentInfo(torrentFileInfoEntityList:List<TorrentFileInfoEntity>?):RemoteTorrentInfo{
    return RemoteTorrentInfo(
        file_count = this.fileCount,
        info_hash = this.hash,
        is_multi_files = this.isMultiFiles,
        name = File(this.torrentPath).nameWithoutExtension,
        sub_fileinfo = torrentFileInfoEntityList?.map {
            RemoteSubFileInfo(
                file_index = it.fileIndex,
                file_name = it.fileName,
                file_size = it.fileSize,
                is_selected = TaskTools.isVideoFile(it.fileName)
            )
        } ?: emptyList(),
        url = this.torrentPath
    )
}
