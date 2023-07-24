package com.station.stationdownloader.utils

import com.xunlei.downloadlib.XLTaskHelper

object XLEngineTools {

    fun assertTorrentFile(path:String):Boolean{
        val torrentInfo=XLTaskHelper.instance().getTorrentInfo(path)
        return torrentInfo.mInfoHash != null
    }
}