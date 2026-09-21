package com.example.wifianalyzeriot.util

import android.net.Network
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit

object LatencyTester {
    private const val TAG = "LatencyTester"

    /**
     * Testa a latência usando uma abordagem híbrida: Ping (ICMP), Sockets TCP ou HTTPS.
     */
    suspend fun testLatency(network: Network? = null, host: String = "8.8.8.8"): Long? = withContext(Dispatchers.IO) {
        if (host.isEmpty() || host == "0.0.0.0") return@withContext null
        
        Log.d(TAG, "Iniciando teste para: $host")

        // 1. Tenta Ping (ICMP) - Comando padrão
        try {
            val process = ProcessBuilder("ping", "-c", "1", "-w", "2", host)
                .redirectErrorStream(true)
                .start()
            
            var latency: Long? = null
            process.inputStream.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    if (line.contains("time=")) {
                        val match = "time=([\\d[.,]]+)".toRegex().find(line)
                        match?.groupValues?.get(1)?.let {
                            latency = it.replace(",", ".").toDouble().toLong()
                        }
                    }
                }
            }
            process.waitFor(3, TimeUnit.SECONDS)
            if (latency != null) return@withContext latency
        } catch (e: Exception) {
            Log.e(TAG, "Ping falhou: ${e.message}")
        }

        // 2. Fallback: Socket TCP
        val commonPorts = listOf(53, 80, 443)
        for (port in commonPorts) {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                network?.bindSocket(socket)
                socket.connect(InetSocketAddress(host, port), 2000)
                val elapsed = System.currentTimeMillis() - start
                socket.close()
                return@withContext elapsed
            } catch (e: Exception) { }
        }

        // 3. Fallback final: HTTPS Check
        if (host == "8.8.8.8" || host.contains("google")) {
            try {
                val start = System.currentTimeMillis()
                val url = URL("https://www.google.com/generate_204")
                val conn = (if (network != null) network.openConnection(url) else url.openConnection()) as HttpURLConnection
                conn.connectTimeout = 3000
                conn.connect()
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..399) return@withContext System.currentTimeMillis() - start
            } catch (e: Exception) { }
        }

        null
    }
}
