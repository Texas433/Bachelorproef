package com.example.new_test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.bluetooth.BluetoothDevice

class DeviceListAdapter(private var devices: List<BluetoothDevice>,private val onDeviceClick: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder>() {

    // ViewHolder voor het apparaatitem
    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(android.R.id.text1)
    }

    // Creëer een nieuwe ViewHolder voor elk item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeviceViewHolder(view)
    }

    // Koppel het apparaat aan de ViewHolder
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceName.text = device.name ?: "Onbekend apparaat"
        holder.itemView.setOnClickListener {
            onDeviceClick(device) // Roep de onDeviceClick callback aan wanneer het apparaat wordt geklikt
        }
    }

    // Aantal items in de lijst
    override fun getItemCount(): Int = devices.size

    // Voeg een apparaat toe aan de lijst
    fun updateDeviceList(newDevices: List<BluetoothDevice>) {
        // Werk de lijst bij met de nieuwe apparaten
        devices = newDevices
        notifyDataSetChanged()  // Herlaad de RecyclerView
    }

    // Haal de huidige lijst van apparaten op
    fun getDevices(): List<BluetoothDevice> {
        return devices
    }
}
