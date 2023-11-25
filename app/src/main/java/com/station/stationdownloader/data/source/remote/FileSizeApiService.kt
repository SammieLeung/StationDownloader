package com.station.stationdownloader.data.source.remote

import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.remote.api.FileSizeApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FileSizeApiService(
    private val fileSizeApi: FileSizeApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getHttpFileHeader(url: String): FileContentHeader = withContext(ioDispatcher) {
        suspendCoroutine { continuation ->
            fileSizeApi.getFileHeader(url).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        Logger.d(response.headers().toString())
                        val contentLength = response.headers().get("Content-Length") ?: "-1"
                        val contentType = response.headers().get("Content-Type") ?: ""
                        continuation.resume(
                            FileContentHeader(
                                url = url,
                                content_length = contentLength.toLong(),
                                content_type = contentType
                            )
                        )
                    } else if (response.code() == 302) {
                        val location = response.headers().get("Location") ?: ""
                        launch(ioDispatcher) {
                            val header = getHttpFileHeader(location)
                            continuation.resume(header)
                        }

                    } else {
                        continuation.resumeWithException(Exception("请求失败"))
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    continuation.resumeWithException(t)
                }
            })
        }

    }
}

data class FileContentHeader(
    val url:String,
    val content_length: Long,
    val content_type: String
)
