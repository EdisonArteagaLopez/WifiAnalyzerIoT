package com.example.wifianalyzeriot.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.wifianalyzeriot.R
import com.example.wifianalyzeriot.databinding.ItemWifiNetworkBinding
import com.example.wifianalyzeriot.model.WifiNetworkInfo

class WifiNetworkAdapter(
    private var networks: List<WifiNetworkInfo>
) : RecyclerView.Adapter<WifiNetworkAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemWifiNetworkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWifiNetworkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val net = networks[position]
        val context = holder.itemView.context
        
        // Use resources to avoid lint warnings and support localization
        holder.binding.tvSsid.text = net.ssid.ifEmpty { context.getString(R.string.hidden_network) }
        holder.binding.tvBssid.text = net.bssid
        
        holder.binding.tvDetails.text = context.getString(
            R.string.wifi_details_format,
            net.rssi,
            net.frequency,
            net.channel,
            net.security
        )
    }

    override fun getItemCount() = networks.size

    fun updateList(newList: List<WifiNetworkInfo>) {
        networks = newList
        notifyDataSetChanged()
    }
}
