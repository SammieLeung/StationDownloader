package com.station.stationdownloader.di

import android.util.Log
import com.station.stationdownloader.data.source.remote.BtTrackerApiService
import com.station.stationdownloader.data.source.remote.FileSizeApiService
import com.station.stationdownloader.data.source.remote.api.BtTrackerApi
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager
@Qualifier
annotation class NoCertificationOkhttpClient
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
    @NoCertificationOkhttpClient
    fun provideNoCertificationOkhttpClient(): OkHttpClient {
        val sc = SSLContext.getInstance("SSL")
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        val trustManagers= arrayOf(trustManager)
        sc.init(null, trustManagers, SecureRandom())
        return OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .followRedirects(false)
            .sslSocketFactory(sc.socketFactory, trustManager)
            .hostnameVerifier { _, _ -> true }
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
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
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

    @Provides
    @Singleton
    fun provideBtTrackerApi(
        @NoCertificationOkhttpClient
        client: OkHttpClient
    ): BtTrackerApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://cf.trackerslist.com/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(BtTrackerApi::class.java)
    }

    @Provides
    fun provideBtTrackerApiService(
        btTrackerApi: BtTrackerApi,
        @IoDispatcher
        ioDispatcher: CoroutineDispatcher
    ): BtTrackerApiService {
        return BtTrackerApiService(btTrackerApi, ioDispatcher)
    }
}

