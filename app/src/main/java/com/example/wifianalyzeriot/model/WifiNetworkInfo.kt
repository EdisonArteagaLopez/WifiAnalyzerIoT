package com.example.wifianalyzeriot.model

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val band: String,      // "2.4 GHz" ou "5 GHz"
    val security: String
)