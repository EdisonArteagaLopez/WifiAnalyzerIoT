package com.example.wifianalyzeriot.model

data class ConnectionSnapshot(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val linkSpeed: Int,    // Mbps
    val bandType: String   // "2.4 GHz" ou "5 GHz"
)