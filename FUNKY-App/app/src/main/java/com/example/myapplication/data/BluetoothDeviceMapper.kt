package com.example.myapplication.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.myapplication.domain.chat.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}