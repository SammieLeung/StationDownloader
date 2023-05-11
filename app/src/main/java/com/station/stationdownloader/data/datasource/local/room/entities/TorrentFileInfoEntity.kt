package com.station.stationdownloader.data.datasource.local.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import javax.inject.Inject

@Entity(
    tableName = "torrent_file_info",
    indices = [Index(value = ["torrent_id"], unique = true)]
)
data class TorrentFileInfoEntity @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    @ColumnInfo(name = "torrent_id")
    var torrentId: Long,
    @ColumnInfo(name = "file_index")
    var fileIndex: Int = 0,
    @ColumnInfo(name = "file_name")
    var fileName: String = "",
    @ColumnInfo(name = "file_size")
    var fileSize: Long = 0,
    @ColumnInfo(name = "real_index")
    var realIndex: Int = 0,
    @ColumnInfo(defaultValue = "", name = "sub_path")
    var subPath: String = "",
)
