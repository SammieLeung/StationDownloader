package com.station.stationdownloader

import com.station.stationdownloader.utils.TaskTools
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test

import org.junit.Assert.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }


    fun getFileSize(urlString: String): Long {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        connection.connect()

        val contentLength = connection.getHeaderField("Content-Length")
        val fileSize = contentLength?.toLong() ?: -1

        connection.disconnect()

        return fileSize
    }

    fun decodeBase64(base64:String):String{
        return String(Base64.getDecoder().decode(base64))
    }


    @Test
    fun testGetFileSize(){
//        val fileUrl = "https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2022.2.1.20/android-studio-2022.2.1.20-linux.tar.gz"
        val fileUrl="https://www.bilibili.com/29fddec9-16d6-4228-ac73-ee04be3ab0c5"
        val fileSize = getFileSize(fileUrl)
        println("File size: $fileSize bytes")
    }

    @Test
    fun testBase64Decode(){
        println(decodeBase64("QUFodHRwczovL3JlZGlyZWN0b3IuZ3Z0MS5jb20vZWRnZWRsL2FuZHJvaWQvc3R1ZGlvL2lkZS16aXBzLzIwMjIuMi4xLjIwL2FuZHJvaWQtc3R1ZGlvLTIwMjIuMi4xLjIwLWxpbnV4LnRhci5nelpa"))
        println( TaskTools.thunderLinkDecode("thunder://QUFodHRwczovL3JlZGlyZWN0b3IuZ3Z0MS5jb20vZWRnZWRsL2FuZHJvaWQvc3R1ZGlvL2lkZS16aXBzLzIwMjIuMi4xLjIwL2FuZHJvaWQtc3R1ZGlvLTIwMjIuMi4xLjIwLWxpbnV4LnRhci5nelpa"))
    }
}