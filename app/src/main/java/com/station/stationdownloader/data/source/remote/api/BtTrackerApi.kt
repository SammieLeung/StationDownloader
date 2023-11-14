package com.station.stationdownloader.data.source.remote.api

import retrofit2.Call
import retrofit2.http.GET

interface BtTrackerApi {
    @GET("all_aria2.txt")
    fun getBtTrackerAllList(): Call<String>

    @GET("best_aria2.txt")
    fun getBtTrackerBestList(): Call<String>

    @GET("http_aria2.txt")
    fun getBtTrackerHttpList(): Call<String>
}