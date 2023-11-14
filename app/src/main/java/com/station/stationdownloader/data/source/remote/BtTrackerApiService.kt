package com.station.stationdownloader.data.source.remote

import com.orhanobut.logger.Logger
import com.station.stationdownloader.data.source.remote.api.BtTrackerApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BtTrackerApiService(
    private val btTrackerApi: BtTrackerApi,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getBtTrackerAllList():String = withContext(ioDispatcher) {
        return@withContext suspendCoroutine<String> { continuation ->
            btTrackerApi.getBtTrackerAllList().enqueue(object : Callback<String> {
                override fun onResponse(call: Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        response.body()?.let {
                            continuation.resume(it)
                        }?: continuation.resumeWith(Result.failure(Exception("response body is null")))
                    }else {
                        continuation.resumeWith(Result.failure(Exception("get bt tracker list failed")))
                    }
                }

                override fun onFailure(call: Call<String>, t: Throwable) {
                     continuation.resumeWith(Result.failure(t))
                }

            })
        }

    }
}