package com.station.stationdownloader.data.source.local.engine.aria2.connection.common

import java.security.cert.X509Certificate

data class ConnFields(
    val connectionMethod: ConnectionMethod,
    val address: String,
    val port: Int,
    val endpoint: String,
    val encryption: Boolean = false,
    val certificate: X509Certificate? = null,
    val hostnameVerifier: Boolean = false
) {

    enum class ConnectionMethod {
        HTTP, WEBSOCKET
    }
}
