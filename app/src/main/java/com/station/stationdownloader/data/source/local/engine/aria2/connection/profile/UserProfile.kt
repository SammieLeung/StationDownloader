package com.station.stationdownloader.data.source.local.engine.aria2.connection.profile

import android.util.Base64
import android.util.Log
import com.gianlu.aria2lib.Aria2PK
import com.gianlu.aria2lib.commonutils.Prefs
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.AuthFields
import com.station.stationdownloader.data.source.local.engine.aria2.connection.common.ConnFields
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.Serializable
import java.security.cert.CertificateEncodingException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.ThreadLocalRandom

data class UserProfile private constructor(
    val name: String,
    val id: String = UserProfileManager.getId(name),
    val serverAddr: String,
    val serverPort: Int,
    val serverEndpoint: String,
    val authMethod: AuthFields.AuthMethod,
    val serverSsl: Boolean = false,
    val hostnameVerifier: Boolean = false,
    val certificate: X509Certificate? = null,
    val serverUsername: String? = null,
    val serverPassword: String? = null,
    val serverToken: String? = null,
    val connectionMethod: ConnFields.ConnectionMethod
) : Serializable {

    constructor(
        _name: String,
        authFields: AuthFields,
        connFields: ConnFields
    ) : this(
        name = _name,
        serverAddr = connFields.address,
        serverPort = connFields.port,
        serverEndpoint = connFields.endpoint,
        authMethod = authFields.authMethod,
        serverSsl = connFields.encryption,
        hostnameVerifier = connFields.hostnameVerifier,
        certificate = connFields.certificate,
        serverUsername = when (authFields) {
            is AuthFields.HttpAuthFields -> authFields.username
            else -> null
        },
        serverPassword = when (authFields) {
            is AuthFields.HttpAuthFields -> authFields.password
            else -> null
        },
        serverToken = when (authFields) {
            is AuthFields.TokenAuthFields -> authFields.token
            else -> null
        },
        connectionMethod = connFields.connectionMethod
    )

    constructor(json: JSONObject) : this(
        name = json.getString("name"),
        serverAddr = json.getString("serverAddr"),
        serverPort = json.getInt("serverPort"),
        serverEndpoint = json.getString("serverEndpoint"),
        authMethod = AuthFields.AuthMethod.valueOf(json.getString("authMethod")),
        serverSsl = json.getBoolean("serverSsl"),
        hostnameVerifier = json.getBoolean("hostnameVerifier"),
        certificate = if (json.has("certificate")) decodeCertificate(
            json.getString("certificate")
        ) else null,
        serverUsername = json.optString("serverUsername", null),
        serverPassword = json.optString("serverPassword", null),
        serverToken = json.optString("serverToken", null),
        connectionMethod = ConnFields.ConnectionMethod.valueOf(json.getString("connectionMethod"))
    )

    fun isInAppProfile(): Boolean {
        return name == IN_APP_DOWNLOADER_NAME
    }

    @Throws(JSONException::class)
    fun toJson(): JSONObject {
        val profile = JSONObject()
        profile.put("name", name)
            .put("serverAddr", serverAddr)
            .put("serverPort", serverPort)
            .put("serverEndpoint", serverEndpoint)
            .put("authMethod", authMethod.name)
            .put("serverToken", serverToken)
            .put("serverUsername", serverUsername)
            .put("serverPassword", serverPassword)
            .put("hostnameVerifier", hostnameVerifier)
            .put("serverSsl", serverSsl)
            .put("connectionMethod", connectionMethod.name)
        if (certificate != null && serverSsl) profile.put(
            "certificate",
            encodeCertificate(certificate)
        )
        return profile
    }

    companion object {
        const val IN_APP_DOWNLOADER_NAME = "in_app_downloader"

        @Throws(IllegalArgumentException::class)
        fun randomPort(): Int {
            return ThreadLocalRandom.current().nextInt(2000, 8000)
        }

        @Throws(IllegalArgumentException::class)
        fun randomToken(length: Int): String {
            if (length < 1) throw IllegalArgumentException("length < 1: $length")

            val chars =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"£$%&/()=?^-_.:,;<>|\\*[]"
            val str: StringBuilder = StringBuilder(length)
            val random = ThreadLocalRandom.current()
            for (i in 0 until length) str.append(chars[random.nextInt(chars.length)])
            return str.toString()
        }

        @JvmStatic
        fun forInAppDownloader(): UserProfile {
            val port = randomPort()
            val token: String = randomToken(8)
            Log.d("UserProfile", "forInAppDownloader port=$port token=$token")
            Prefs.putInt(Aria2PK.RPC_PORT, port)
            Prefs.putString(Aria2PK.RPC_TOKEN, token)
            return UserProfile(
                IN_APP_DOWNLOADER_NAME,
                AuthFields.TokenAuthFields(token),
                ConnFields(ConnFields.ConnectionMethod.WEBSOCKET, "localhost", port, "/jsonrpc")
            )
        }

        private fun decodeCertificate(base64: String): X509Certificate? {
            return try {
                val factory = CertificateFactory.getInstance("X.509")
                factory.generateCertificate(
                    ByteArrayInputStream(
                        Base64.decode(
                            base64,
                            Base64.NO_WRAP
                        )
                    )
                ) as X509Certificate
            } catch (ex: CertificateException) {
                Log.e("UserProfileKt", "Failed decoding certificate.")
                null
            }
        }

        private fun encodeCertificate(certificate: X509Certificate): String? {
            return try {
                Base64.encodeToString(certificate.encoded, Base64.NO_WRAP)
            } catch (ex: CertificateEncodingException) {
                Log.e("UserProfileKt", "Failed encoding certificate.")
                null
            }
        }


    }


}
