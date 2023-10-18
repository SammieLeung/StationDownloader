package com.station.stationdownloader.data.source.local.engine.aria2.connection.util

import com.gianlu.aria2lib.commonutils.Prefs
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.Aria2UserPK
import com.station.stationdownloader.data.source.local.engine.aria2.connection.profile.UserProfile
import com.station.stationdownloader.data.source.local.engine.aria2.connection.exception.InvalidUrlException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.Certificate
import java.util.Arrays
import java.util.concurrent.TimeUnit
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object Aria2NetUtils {
    @Throws(InvalidUrlException::class)
    fun createWebSocketURL(profile: UserProfile): URI? {
        return try {
            val uri=URI(
                if (profile.serverSsl) "wss" else "ws",
                null,
                profile.serverAddr,
                profile.serverPort,
                profile.serverEndpoint,
                null,
                null
            )
            uri
        } catch (ex: Exception) {
            throw InvalidUrlException(ex)
        }
    }

    //region SSL
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun getTrustManager(ca: Certificate): X509TrustManager {
        val password = "password".toCharArray()
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, password)
        keyStore.setCertificateEntry("ca", ca)
        val keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, password)
        val trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        val trustManagers = trustManagerFactory.trustManagers
        if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) throw GeneralSecurityException(
            "Unexpected default trust managers:" + Arrays.toString(trustManagers)
        )
        return trustManagers[0] as X509TrustManager
    }

    @Throws(GeneralSecurityException::class)
    private fun getSslSocketFactory(trustManager: TrustManager): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustManager), null)
        return sslContext.socketFactory
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun setSslSocketFactory(client: OkHttpClient.Builder, ca: Certificate) {
        val trustManager: X509TrustManager = getTrustManager(ca)
        val sslSocketFactory: SSLSocketFactory =
            getSslSocketFactory(trustManager)
        client.sslSocketFactory(sslSocketFactory, trustManager)
    }

    @Throws(GeneralSecurityException::class, IOException::class)
    fun buildClient(profile: UserProfile): OkHttpClient {
        val timeout: Int = Prefs.getInt(Aria2UserPK.A2_NETWORK_TIMEOUT)
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeout.toLong(), TimeUnit.SECONDS)
        if (profile.certificate != null) setSslSocketFactory(builder, profile.certificate)
        if (!profile.hostnameVerifier) builder.hostnameVerifier { _, _ -> true }
        return builder.build()
    }

    @Throws(InvalidUrlException::class)
    fun createWebsocketRequest(profile: UserProfile): Request {
        val builder = Request.Builder()
        builder.url(createWebSocketURL(profile).toString())
        return builder.build()
    }
}