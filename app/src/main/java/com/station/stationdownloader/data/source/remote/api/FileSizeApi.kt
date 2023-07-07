package com.station.stationdownloader.data.source.remote.api

import retrofit2.Call
import retrofit2.http.HEAD
import retrofit2.http.Url

interface FileSizeApi {

    @HEAD
    fun getFileHeader(@Url fileUrl: String): Call<Void>
}