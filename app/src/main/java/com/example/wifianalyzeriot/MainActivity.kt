package com.example.wifianalyzeriot

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wifianalyzeriot.R
import com.example.wifianalyzeriot.databinding.ActivityMainBinding
import com.example.wifianalyzeriot.model.WifiNetworkInfo
import com.example.wifianalyzeriot.ui.WifiNetworkAdapter
import com.example.wifianalyzeriot.util.LatencyTester
import com.example.wifianalyzeriot.util.WifiUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var adapter: WifiNetworkAdapter
    private var networksList = mutableListOf<WifiNetworkInfo>()
    
    private var currentWifiNetwork: Network? = null

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateWifiList()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            currentWifiNetwork = network
            runOnUiThread { updateConnectedNetworkInfo() }
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            currentWifiNetwork = network
            runOnUiThread { updateConnectedNetworkInfo() }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            if (currentWifiNetwork == network) currentWifiNetwork = null
            runOnUiThread {
                binding.tvCurrentSsid.text = "SSID: -"
                binding.tvCurrentRssi.text = "RSSI: -"
                binding.tvCurrentSpeed.text = "Link: -"
                binding.tvCurrentBand.text = "Banda: -"
                binding.tvLatency.text = "Latência: -"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setupRecyclerView()
        setupButtons()
        registerReceivers()
        checkPermissionsAndScan()
    }

    private fun setupRecyclerView() {
        adapter = WifiNetworkAdapter(networksList)
        binding.recyclerNetworks.layoutManager = LinearLayoutManager(this)
        binding.recyclerNetworks.adapter = adapter
    }

    private fun setupButtons() {
        binding.btnScan.setOnClickListener { checkPermissionsAndScan() }
        binding.btnLatency.setOnClickListener { testLatency() }
    }

    private fun registerReceivers() {
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(scanReceiver, filter)
        
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun isLocationEnabled(): Boolean {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            val mode = Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
            mode != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    private fun checkPermissionsAndScan() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            if (!isLocationEnabled()) Toast.makeText(this, getString(R.string.location_required), Toast.LENGTH_SHORT).show()
            startWifiScan() 
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { startWifiScan() }

    private fun startWifiScan() {
        if (!wifiManager.isWifiEnabled) {
            Toast.makeText(this, getString(R.string.wifi_disabled), Toast.LENGTH_SHORT).show()
            return
        }
        @Suppress("DEPRECATION")
        val success = wifiManager.startScan()
        if (!success) Toast.makeText(this, getString(R.string.scan_throttled), Toast.LENGTH_SHORT).show()
    }

    private fun updateWifiList() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val results = try { wifiManager.scanResults } catch (e: SecurityException) { null } ?: return
        networksList.clear()
        for (result in results) {
            if (result.SSID.isNotEmpty()) networksList.add(WifiUtils.scanResultToNetworkInfo(result))
        }
        networksList.sortByDescending { it.rssi }
        adapter.updateList(networksList)
        binding.channelGraphView.updateData(networksList)
    }

    private fun updateConnectedNetworkInfo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        @Suppress("DEPRECATION")
        val connectionInfo = wifiManager.connectionInfo
        val ssid = connectionInfo.ssid?.removeSurrounding("\"") ?: getString(R.string.unknown_ssid)
        val bssid = connectionInfo.bssid ?: "N/A"
        val rssi = connectionInfo.rssi
        val linkSpeed = connectionInfo.linkSpeed
        val freq = connectionInfo.frequency
        val band = if (freq in 2400..2500) "2.4 GHz" else "5 GHz"
        
        binding.tvCurrentSsid.text = "SSID: $ssid"
        binding.tvCurrentRssi.text = "RSSI: $rssi dBm"
        binding.tvCurrentSpeed.text = "Link: $linkSpeed Mbps"
        binding.tvCurrentBand.text = "Banda: $band"
    }

    private fun testLatency() {
        lifecycleScope.launch {
            binding.tvLatency.text = getString(R.string.latency_testing)
            
            @Suppress("DEPRECATION")
            val wifiNetwork = currentWifiNetwork ?: connectivityManager.allNetworks.find { 
                connectivityManager.getNetworkCapabilities(it)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true 
            }

            if (wifiNetwork == null) {
                binding.tvLatency.text = getString(R.string.latency_error_wifi)
                return@launch
            }

            val linkProps = connectivityManager.getLinkProperties(wifiNetwork)
            var gatewayIp = linkProps?.routes?.mapNotNull { it.gateway?.hostAddress }?.firstOrNull()
            
            if (gatewayIp == null || gatewayIp == "0.0.0.0") {
                @Suppress("DEPRECATION")
                gatewayIp = WifiUtils.getGatewayIp(wifiManager.dhcpInfo)
            }

            val target = if (gatewayIp != null && gatewayIp != "0.0.0.0") gatewayIp else "8.8.8.8"
            Log.d("Latency", "Destino: $target")

            val latency = LatencyTester.testLatency(wifiNetwork, target)

            if (latency != null) {
                binding.tvLatency.text = getString(R.string.latency_success, latency, target)
            } else {
                binding.tvLatency.text = getString(R.string.latency_fail)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(scanReceiver) } catch (e: Exception) {}
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
