package com.example.ham_radio_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.getDefaultAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ham_radio_app.ui.theme.HAMRadioAppTheme

class MainActivity : ComponentActivity() {

    private val PERMISSION_CODE = 1
    private val bluetoothAdapter: BluetoothAdapter = getDefaultAdapter()

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.i("Bluetooth", ":request permission result ok")
        } else {
            Log.i("Bluetooth", ":request permission result canceled / denied")
        }
    }

    private fun requestBluetoothPermission() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activityResultLauncher.launch(enableBluetoothIntent)
    }

    @SuppressLint("MissingPermission")
    var pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
    var discoveredDevices: Set<BluetoothDevice> = emptySet()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        val updated = discoveredDevices.plus(device)
                        discoveredDevices = updated
                    }
                    Log.i("Bluetooth", "onRecieve: Device found")
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i("Bluetooth", "onRecieve: Started Discovery")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("Bluetooth", "onRecieve: Finished Discovery")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.M)
    fun scan(): Set<BluetoothDevice> {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
        } else {
            bluetoothAdapter.startDiscovery()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            bluetoothAdapter.cancelDiscovery()
        }, 10000L)
        return discoveredDevices
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val startFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        val endFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, foundFilter);
        registerReceiver(receiver, startFilter);
        registerReceiver(receiver, endFilter);

        if (!bluetoothAdapter.isEnabled) {
            requestBluetoothPermission()
        }

        if (SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(
                    baseContext, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    PERMISSION_CODE
                )
            }
        }

        setContent {
            var devices: Set<BluetoothDevice> by remember { mutableStateOf(emptySet()) }

            HAMRadioAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Scaffold(topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Bluetooth Connected List",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(onClick = { devices = scan() }) {
                                Text(
                                    text = "Scan",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Paired Devices",
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            pairedDevices.forEach { device ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                    //elevation = 10.dp
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(text = device.name)
                                        Text(text = device.address)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        if  (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
        unregisterReceiver(receiver)
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HAMRadioAppTheme {

    }
}