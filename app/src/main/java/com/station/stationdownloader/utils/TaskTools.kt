package com.station.stationdownloader.utils

import com.station.stationdownloader.DownloadUrlType
import com.station.stationdownloader.DownloadWorker
import com.station.stationdownloader.data.source.IEngineRepository
import com.station.stationkitkt.MimeTypeHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.Base64
import javax.inject.Inject

const val MAGNET_PROTOCOL = "magnet:?xt=urn:btih:"
const val HTTP_PROTOCOL = "http://"
const val HTTPS_PROTOCOL = "https://"
const val THUNDER_PROTOCOL = "thunder://"
const val FTP_PROTOCOL = "ftp://"
const val ED2K_PROTOCOL = "ed2k://"

object TaskTools {
    /**
     * 解码迅雷链接为普通连接
     */
    fun thunderLinkDecode(thunder: String): String {
        if (!thunder.startsWith(THUNDER_PROTOCOL))
            return thunder
        val base64 = thunder.split(THUNDER_PROTOCOL)[1]
        val link = String(Base64.getDecoder().decode(base64))
        return link.substring(2, link.length - 2)
    }

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
            str.startsWith(MAGNET_PROTOCOL, true) ||
            str.startsWith(THUNDER_PROTOCOL, true) ||
            str.startsWith(ED2K_PROTOCOL, true) ||
            str.startsWith(FTP_PROTOCOL, true) ||
            str.startsWith(HTTP_PROTOCOL, true) ||
            str.startsWith(HTTPS_PROTOCOL, true)
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
        return (uri.startsWith(THUNDER_PROTOCOL) ||
                url.startsWith(FTP_PROTOCOL) ||
                url.startsWith(HTTP_PROTOCOL) ||
                url.startsWith(HTTPS_PROTOCOL) ||
                url.startsWith(ED2K_PROTOCOL) ||
                url.startsWith(MAGNET_PROTOCOL))
    }

    fun isMagnetHash(hash: String): Boolean {
        // 磁力连接中的hash码是一个40位的十六进制字符串
        val magnetHashRegex = "^[a-fA-F0-9]{40}$".toRegex()
        return magnetHashRegex.matches(hash)
    }

    /**
     * 获取Url Type
     */
    fun getUrlType(url: String): DownloadUrlType {
        if (url.isEmpty())
            return DownloadUrlType.UNKNOWN
        val uri = url.lowercase()
        if (uri.startsWith(THUNDER_PROTOCOL))
            return DownloadUrlType.THUNDER
        if (uri.startsWith(HTTP_PROTOCOL) || uri.startsWith(HTTPS_PROTOCOL))
            return DownloadUrlType.HTTP
        if (uri.startsWith(FTP_PROTOCOL))
            return DownloadUrlType.DIRECT
        if (uri.startsWith(MAGNET_PROTOCOL))
            return DownloadUrlType.MAGNET
        if (uri.startsWith(ED2K_PROTOCOL))
            return DownloadUrlType.ED2k
        return DownloadUrlType.UNKNOWN
    }

    /**
     * 是否是媒体文件
     */
    fun isVideoFile(fileName: String): Boolean {
//        return when (fileName.substringAfter('.', "")) {
//            "avi", "mp4", "m4v",
//            "mkv", "mov", "mpeg",
//            "mpg", "mpe", "rm",
//            "rmvb", "3gp", "wmv",
//            "asf", "asx", "dat",
//            "vob", "m3u8", "webm",
//            "flv","ts"-> true
//            else -> false
//        }
        return MimeTypeHelper.isVideo(fileName.substringAfter(".", ""))
    }

    fun isAudioFile(fileName: String): Boolean {
        return MimeTypeHelper.isAudio(fileName.substringAfter(".", ""))
    }


    fun isImageFile(fileName: String): Boolean {
        return MimeTypeHelper.isImage(fileName.substringAfter(".", ""))
    }

    fun isCompress(fileName: String):Boolean{
             return when (fileName.substringAfter('.', "")) {
            "rar", "gz", "7z",
            "zip", "tar","tgz" -> true

                 else -> false
             }
    }


    fun deSelectedIndexes(fileCount: Int, selectIndexes: List<Int>): List<Int> {
        val deSelectedIndexes = mutableListOf<Int>()
        for (i in 0 until fileCount) {
            if (selectIndexes.contains(i).not()) {
                deSelectedIndexes.add(i)
            }
        }
        return deSelectedIndexes
    }

    fun deSelectedIndexes(fileCount: Int, selectIndexes: IntArray): IntArray {
        val deSelectedIndexes = mutableListOf<Int>()
        for (i in 0 until fileCount) {
            if (selectIndexes.isEmpty() || selectIndexes.contains(i).not()) {
                deSelectedIndexes.add(i)
            }
        }
        return deSelectedIndexes.toIntArray()
    }

    fun getExt(name: String): String {
        return name.substringAfterLast('.', "")
    }

    fun toHumanReading(byte: Long): String {
        if (byte >= 1.TB)
            return "${String.format("%.2f", byte.asTB)}TB"
        if (byte >= 1.GB)
            return "${String.format("%.2f", byte.asGB)}GB"
        if (byte >= 1.MB)
            return "${String.format("%.2f", byte.asMB)}MB"
        if (byte >= 1.KB)
            return "${String.format("%.2f", byte.asKB)}KB"
        return "${String.format("%.2f", byte.asByte)}B"
    }

}


val Long.Byte: Double
    get() {
        return this.toDouble()
    }
val Int.Byte: Double
    get() {
        return this.toLong().Byte
    }

val Long.KB: Double
    get() {
        return (this * 1024).Byte
    }
val Int.KB: Double
    get() {
        return this.toLong().KB
    }

val Long.MB: Double
    get() {
        return (this * 1024).KB
    }
val Int.MB: Double
    get() {
        return this.toLong().MB
    }

val Long.GB: Double
    get() {
        return (this * 1024).MB
    }
val Int.GB: Double
    get() {
        return this.toLong().GB
    }

val Long.TB: Double
    get() {
        return (this * 1024).GB
    }
val Int.TB: Double
    get() {
        return this.toLong().TB
    }

val Long.asByte: Double
    get() {
        return this.toDouble()
    }
val Int.asByte: Double
    get() {
        return this.toLong().asByte
    }

val Long.asKB: Double
    get() {
        return this.asByte / 1024
    }
val Int.asKB: Double
    get() {
        return this.toLong().asKB
    }
val Long.asMB: Double
    get() {
        return this.asKB / 1024
    }
val Int.asMB: Double
    get() {
        return this.toLong().asMB
    }
val Long.asGB: Double
    get() {
        return this.asMB / 1024
    }
val Int.asGB: Double
    get() {
        return this.toLong().asGB
    }

val Long.asTB: Double
    get() {
        return this.asGB / 1024
    }
val Int.asTB: Double
    get() {
        return this.toLong().asTB
    }
