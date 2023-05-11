package com.station.pluginscenter.util

import com.station.stationkitkt.SystemPropertiesReflect
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

/**
 * author: Sam Leung
 * date:  2023/2/20
 */

object NetworkUtil {

    fun getIpAddress(): String {
        return getIpAddress(SystemPropertiesReflect.get("ro.net.eth_primary", "eth0"))
            ?: getIpAddress("wlan0") ?: "127.0.0.1"
    }

    private fun getIpAddress(ipType: String): String? {
        var hostIp: String? = null
        try {
            val nis: Enumeration<*> = NetworkInterface.getNetworkInterfaces()
            var ia: InetAddress? = null
            while (nis.hasMoreElements()) {
                val ni = nis.nextElement() as NetworkInterface
                if (ni.name == ipType) {
                    val ias = ni.inetAddresses
                    while (ias.hasMoreElements()) {
                        ia = ias.nextElement()
                        if (ia is Inet6Address) {
                            continue  // skip ipv6
                        }
                        val ip = ia.hostAddress

                        // 过滤掉127段的ip地址
                        if ("127.0.0.1" != ip) {
                            hostIp = ia.hostAddress
                            break
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        }
        return hostIp
    }
}

data class IpAddressInfo(
    val ip: String,
    val type: String,
)