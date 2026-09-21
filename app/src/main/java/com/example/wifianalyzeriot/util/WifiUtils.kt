package com.example.wifianalyzeriot.util

import android.net.DhcpInfo
import android.net.wifi.ScanResult
import com.example.wifianalyzeriot.model.WifiNetworkInfo
import java.util.Locale

object WifiUtils {

    fun getChannelFromFrequency(freq: Int): Int {
        return when {
            freq in 2412..2484 -> (freq - 2412) / 5 + 1
            freq in 5180..5320 -> (freq - 5180) / 5 + 36
            freq in 5500..5700 -> (freq - 5500) / 5 + 100
            freq in 5745..5825 -> (freq - 5745) / 5 + 149
            else -> 0
        }
    }

    fun getBandFromFrequency(freq: Int): String {
        return if (freq in 2400..2500) "2.4 GHz" else "5 GHz"
    }

    fun getSecurity(capabilities: String): String {
        return when {
            capabilities.contains("WPA3") -> "WPA3"
            capabilities.contains("WPA2") -> "WPA2"
            capabilities.contains("WPA") -> "WPA"
            capabilities.contains("WEP") -> "WEP"
            else -> "Aberta"
        }
    }

    fun scanResultToNetworkInfo(result: ScanResult): WifiNetworkInfo {
        val freq = result.frequency
        return WifiNetworkInfo(
            ssid = if (result.SSID.isNullOrEmpty()) "Rede oculta" else result.SSID,
            bssid = result.BSSID,
            rssi = result.level,
            frequency = freq,
            channel = getChannelFromFrequency(freq),
            band = getBandFromFrequency(freq),
            security = getSecurity(result.capabilities)
        )
    }

    fun getGatewayIp(dhcpInfo: DhcpInfo?): String? {
        if (dhcpInfo == null || dhcpInfo.gateway == 0) return null
        val gateway = dhcpInfo.gateway
        return String.format(
            Locale.US,
            "%d.%d.%d.%d",
            gateway and 0xff,
            gateway shr 8 and 0xff,
            gateway shr 16 and 0xff,
            gateway shr 24 and 0xff
        )
    }
}
