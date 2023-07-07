package com.station.stationdownloader.di

import android.util.Log
import com.station.stationdownloader.data.source.remote.FileSizeApiService
import com.station.stationdownloader.data.source.remote.api.FileSizeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HttpModule {

    @Provides
    fun provideOkhttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .addInterceptor(HttpLoggingInterceptor { message ->
                Log.d(
                    "OkHttpClient",
                    "log: $message"
                )
            }
                .setLevel(
                    HttpLoggingInterceptor.Level.BODY
                ))
            .build()
    }

    @Provides
    fun provideFileSizeApi(
        client: OkHttpClient
    ): FileSizeApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://localhost/")
//            .addConverterFactory(ScalarsConverterFactory.create())
//            .client(client)
            .build()
        return retrofit.create(FileSizeApi::class.java)
    }

    @Provides
    fun provideFileSizeApiService(
        fileSizeApi: FileSizeApi,
        @IoDispatcher
        ioDispatcher: CoroutineDispatcher
    ): FileSizeApiService {
        return FileSizeApiService(fileSizeApi, ioDispatcher)
    }
}