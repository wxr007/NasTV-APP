package com.nastv.utils

import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * 局域网相关工具。
 */
object NetworkUtils {

    private const val TAG = "NetworkUtils"

    /**
     * 取本机可用于局域网访问的 IPv4 地址。
     * 优先 wlan（Wi-Fi），其次 eth（有线），都没有则取第一个非回环地址。
     */
    fun localIpv4Address(): String? {
        val candidates = mutableListOf<Pair<String, String>>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return@runCatching
            Collections.list(interfaces).forEach { nif ->
                if (!nif.isUp || nif.isLoopback) return@forEach
                Collections.list(nif.inetAddresses).forEach { addr ->
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        addr.hostAddress?.let { candidates.add(nif.name to it) }
                    }
                }
            }
        }.onFailure { Log.w(TAG, "枚举网卡失败: ${it.message}") }

        if (candidates.isEmpty()) return null
        val preferred = candidates.firstOrNull { it.first.startsWith("wlan") }
            ?: candidates.firstOrNull { it.first.startsWith("eth") }
        return (preferred ?: candidates.first()).second
    }
}
