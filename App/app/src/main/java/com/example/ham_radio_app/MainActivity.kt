package com.example.ham_radio_app

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ham_radio_app.ui.theme.HAMRadioAppTheme

class MainActivity : ComponentActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var pairedDevices: Set<BluetoothDevice> = emptySet()
    private var discoveredDevices: Set<BluetoothDevice> = emptySet()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        discoveredDevices = discoveredDevices.plus(device)
                    }
                    Log.i("Bluetooth", "onReceive: Device found")
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i("Bluetooth", "onReceive: Started Discovery")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i("Bluetooth", "onReceive: Finished Discovery")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun scan(): Set<BluetoothDevice> {
        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) {
                adapter.cancelDiscovery()
            }
            adapter.startDiscovery()
            Handler(Looper.getMainLooper()).postDelayed({
                adapter.cancelDiscovery()
            }, 10000L)
        }
        return discoveredDevices
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val foundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val startFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        val endFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, foundFilter)
        registerReceiver(receiver, startFilter)
        registerReceiver(receiver, endFilter)

        setContent {
            var devices: Set<BluetoothDevice> by remember { mutableStateOf(emptySet()) }
            var hasPermission by remember { mutableStateOf(false) }

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
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 70.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (hasPermission) {
                                Button(onClick = { devices = scan() }) {
                                    Text(
                                        text = "Scan",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Paired Devices",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                pairedDevices.forEach { device ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(text = device.name ?: "Unknown Device")
                                            Text(text = device.address)
                                        }
                                    }
                                }
                            } else {
                                RequestBluetoothPermission { granted ->
                                    hasPermission = granted
                                    if (granted) {
                                        bluetoothAdapter?.let { adapter ->
                                            if (!adapter.isEnabled) {
                                                val enableBtIntent =
                                                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                                startActivityForResult(enableBtIntent, 1)
                                            } else {
                                                pairedDevices = adapter.bondedDevices
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RequestBluetoothPermission(onPermissionResult: (Boolean) -> Unit) {
        val context = LocalContext.current
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val allPermissionsGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.values.all { it }
            onPermissionResult(granted)
        }

        LaunchedEffect(key1 = Unit) {
            if (allPermissionsGranted) {
                onPermissionResult(true)
            } else {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.let { adapter ->
            if (adapter.isDiscovering) adapter.cancelDiscovery()
        }
        unregisterReceiver(receiver)
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HAMRadioAppTheme {
    }
}
