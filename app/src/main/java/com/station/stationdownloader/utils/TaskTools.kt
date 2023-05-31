package com.station.stationdownloader.utils

import com.station.stationdownloader.DownloadUrlType
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder

object TaskTools {
    /**
     * URL解码
     */
    fun urlDecode(str: String): String {
        try {
            var newStr = URLDecoder.decode(str, "UTF-8")
            if (newStr != str)
                return urlDecode(newStr)
            return newStr
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return ""
    }

    fun getUrlDecodeUrl(str: String): String {
        return if (
            str.startsWith("magnet:?xt=urn:btih:", true) ||
            str.startsWith("thunder://", true) ||
            str.startsWith("ed2k://", true) ||
            str.startsWith("ftp://", true) ||
            str.startsWith("http://", true) ||
            str.startsWith("https://", true)
        ) urlDecode(str) else str
    }

    /**
     * 是否支持的连接或种子
     */
    fun isSupportUrl(url: String): Boolean {
        return isSupportNetworkUrl(url) || isTorrentFile(url)
    }

    fun isTorrentFile(url: String): Boolean {
        if (url.isEmpty())
            return false
        val torrentFile = File(url)
        return torrentFile.exists() && torrentFile.extension == "torrent"
    }

    /**
     * 是否支持的网络连接
     */
    fun isSupportNetworkUrl(url: String): Boolean {
        if (url.isEmpty())
            return false
        val uri = url.lowercase()
        return (uri.startsWith("thunder://") || url.startsWith("ftp://") || url.startsWith("http://")
                || url.startsWith("https://") || url.startsWith("ed2k://") || url.startsWith("magnet:?xt=urn:btih:"))
    }

    /**
     * 获取Url Type
     */
    fun getUrlType(url: String): DownloadUrlType {
        if (url.isEmpty())
            return DownloadUrlType.UNKNOWN
        val uri = url.lowercase()
        if (uri.startsWith("thunder://"))
            return DownloadUrlType.THUNDER
        if (uri.startsWith("http://") || uri.startsWith("https://"))
            return DownloadUrlType.HTTP
        if (uri.startsWith("ftp://"))
            return DownloadUrlType.DIRECT
        if (uri.startsWith("magnet:?xt=urn:btih:"))
            return DownloadUrlType.META_LINK
        if (uri.startsWith("ed2k://"))
            return DownloadUrlType.ED2k
        return DownloadUrlType.UNKNOWN
    }

    /**
     * 是否是媒体文件
     */
    fun isMediaFile(fileName: String): Boolean {
        return when (fileName.substringAfterLast('.', "")) {
            "avi", "mp4", "m4v",
            "mkv", "mov", "mpeg",
            "mpg", "mpe", "rm",
            "rmvb", "3gp", "wmv",
            "asf", "asx", "dat",
            "vob", "m3u8", "webm" -> true

            else -> false
        }
    }
}

val Long.Byte: Long
    get() {
        return this
    }
val Int.Byte: Long
    get() {
        return this.toLong().Byte
    }

val Long.KB: Long
    get() {
        return this.Byte * 1024L
    }
val Int.KB: Long
    get() {
        return this.toLong().KB
    }

val Long.MB: Long
    get() {
        return this.KB * 1024L
    }
val Int.MB: Long
    get() {
        return this.toLong().MB
    }

val Long.GB: Long
    get() {
        return this.MB * 1024L
    }
val Int.GB: Long
    get() {
        return this.toLong().GB
    }

val Long.TB: Long
    get() {
        return this.GB * 1024L
    }
val Int.TB: Long
    get() {
        return this.toLong().TB
    }