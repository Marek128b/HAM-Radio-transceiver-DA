package com.example.myapplication.domain.chat

import android.bluetooth.BluetoothGatt

typealias BluetoothDeviceDomain = BluetoothDevice

data class BluetoothDevice(
    val name: String?,
    val address: String
)
