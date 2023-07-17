package com.station.stationkitkt

import android.content.Context
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

object MimeTypeHelper {
    val hasRun:AtomicBoolean= AtomicBoolean(false)
    var videoMimeList = mutableListOf<String>()
    var audioMimeList = mutableListOf<String>()
    var imageMimeList = mutableListOf<String>()
    var otherList = mutableListOf<String>()


    fun init(context: Context) {
        if(hasRun.compareAndSet(false,true)) {
            val inputStream = context.resources.openRawResource(R.raw.mimetypes)

            val parser = MimeTypeXmlParser()
            val mimeTypeMap = parser.parse(inputStream)
            filter(mimeTypeMap, "video/").forEach {
                videoMimeList.add(it)
            }
            filter(mimeTypeMap, "audio/").forEach {
                audioMimeList.add(it)
            }
            filter(mimeTypeMap, "image/").forEach {
                imageMimeList.add(it)
            }
            mimeTypeMap.filter {
                !(it.value.startsWith("video/")
                        || it.value.startsWith("audio/")
                        || it.value.startsWith("image/")
                        )
            }.map {
                it.key.substringAfterLast(".")
            }.forEach { otherList.add(it) }
        }
    }

    fun isVideo(ext: String): Boolean {
        val data=ext.substringAfterLast(".")
        val boo= videoMimeList.contains(data)
        return boo
    }

    fun isAudio(ext: String): Boolean {
        return audioMimeList.contains(ext.substringAfterLast("."))
    }

    fun isImage(ext: String): Boolean {
        return imageMimeList.contains(ext.substringAfterLast("."))
    }

    private fun filter(source: Map<String, String>, tag: String): List<String> {
        return source.filter {
            it.value.startsWith(tag)
        }.map {
            it.key.substringAfterLast(".")
        }
    }


    class MimeTypeXmlParser {
        // We don't use namespaces.
        private val ns: String? = null

        @Throws(XmlPullParserException::class, IOException::class)
        fun parse(inputStream: InputStream): Map<String, String> {
            inputStream.use { inputStream ->
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(inputStream, null)
                parser.nextTag()
                val result= readHead(parser)
                inputStream.close()
                return result
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun readHead(parser: XmlPullParser): Map<String, String> {
            val map = mutableMapOf<String, String>()
            parser.require(XmlPullParser.START_TAG, ns, "mimeTypes")
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                // Starts by looking for the entry tag.
                if (parser.name == "mimeType") {
                    readMimeType(parser, map)
                } else {
                    skip(parser)
                }
            }
            return map

        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun skip(parser: XmlPullParser) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException()
            }
            var depth = 1
            while (depth != 0) {
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
        }

        private fun readMimeType(
            parser: XmlPullParser,
            map: MutableMap<String, String>
        ) {
            parser.require(XmlPullParser.START_TAG, ns, "mimeType")
            var extention: String? = ""
            var mime: String? = null
            val tag = parser.name

            if (tag == "mimeType") {
                extention = parser.getAttributeValue(null, "extension")
                mime = parser.getAttributeValue(null, "type")
                map.put(extention, mime)
                parser.nextTag()
            }
            parser.require(XmlPullParser.END_TAG, ns, "mimeType")
        }
    }
}

