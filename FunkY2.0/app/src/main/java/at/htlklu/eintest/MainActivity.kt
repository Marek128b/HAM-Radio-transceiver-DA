package at.htlklu.eintest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID für SPP
    private var selectedDevice: BluetoothDevice? = null
    private var outputStream: OutputStream? = null
    private var isScanning by mutableStateOf(false)

    private val requestBluetoothPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        permissions.entries.forEach {
            Log.d("Bluetooth", "${it.key} = ${it.value}")
        }
        if (permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true &&
            permissions[android.Manifest.permission.BLUETOOTH_SCAN] == true
        ) {
            Log.d("Bluetooth", "Berechtigungen gewährt. Bluetooth kann verwendet werden.")
        } else {
            Log.d("Bluetooth", "Berechtigungen verweigert. Bluetooth kann nicht verwendet werden.")
            Toast.makeText(this, "Bluetooth-Berechtigungen erforderlich", Toast.LENGTH_SHORT).show()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (bluetoothAdapter?.isEnabled == true) {
            Toast.makeText(this, "Bluetooth aktiviert", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Bluetooth nicht aktiviert", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothApp()
        }

        checkAndRequestBluetoothPermissions()
        promptEnableBluetooth()
    }

    private fun checkAndRequestBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestBluetoothPermissions.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun promptEnableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    private fun getPairedDevices(): List<String> {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        val deviceList = mutableListOf<String>()
        pairedDevices?.forEach { device ->
            deviceList.add("${device.name} (${device.address})")
        }
        return deviceList
    }

    private fun startScanningDevices(scannedDevices: MutableState<List<String>>) {
        if (isScanning) {
            bluetoothAdapter?.cancelDiscovery()
            isScanning = false
            Toast.makeText(this, "Scan gestoppt", Toast.LENGTH_SHORT).show()
        } else {
            if (bluetoothAdapter?.startDiscovery() == true) {
                isScanning = true
                Toast.makeText(this, "Scan gestartet", Toast.LENGTH_SHORT).show()
                registerReceiver(
                    object : android.content.BroadcastReceiver() {
                        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                device?.let {
                                    if (!it.name.isNullOrEmpty()) { // Nur Geräte mit Namen hinzufügen
                                        scannedDevices.value = scannedDevices.value + "${it.name} (${it.address})"
                                    }
                                }
                            }
                        }
                    },
                    android.content.IntentFilter(BluetoothDevice.ACTION_FOUND)
                )
            } else {
                Toast.makeText(this, "Scan konnte nicht gestartet werden", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(deviceUUID)
            bluetoothAdapter?.cancelDiscovery() // Wichtig, um Verbindungsprobleme zu vermeiden
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(this, "Verbindung zu ${device.name} hergestellt!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Verbindungsfehler: ${e.message}")
            Toast.makeText(this, "Verbindungsfehler: ${e.message}", Toast.LENGTH_SHORT).show()
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e("Bluetooth", "Fehler beim Schließen des Sockets: ${closeException.message}")
            }
        }
    }

    private fun sendHelloWorldToESP() {
        try {
            if (bluetoothSocket?.isConnected == true) {
                val message = "ENDLIIIIIIIIIIIIIIIIIIIIIIIIIIICHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH"
                outputStream?.write(message.toByteArray())
                Toast.makeText(this, "Nachricht gesendet: $message", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth ist nicht verbunden", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Fehler beim Senden der Nachricht: ${e.message}")
            Toast.makeText(this, "Fehler beim Senden der Nachricht: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startListeningForData(receivedData: MutableState<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = bluetoothSocket?.inputStream
                if (inputStream == null) {
                    Log.e("Bluetooth", "InputStream ist null")
                    return@launch
                }
                val buffer = ByteArray(1024)
                while (bluetoothSocket?.isConnected == true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead)
                        Log.d("Bluetooth", "Empfangene Daten: $data")
                        withContext(Dispatchers.Main) {
                            receivedData.value += "$data\n"
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e("Bluetooth", "Fehler beim Lesen von Daten: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close() // Schließe die Verbindung beim Beenden der App
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Fehler beim Schließen des Sockets: ${e.message}")
        }
    }

    @Composable
    fun BluetoothApp() {
        val pairedDevices by remember { mutableStateOf(getPairedDevices()) }
        val scannedDevices = remember { mutableStateOf(listOf<String>()) }
        val receivedData = remember { mutableStateOf("") }
        var selectedDeviceName by remember { mutableStateOf<String?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Empfangene Daten:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(8.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = receivedData.value,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(2f)) {
                item {
                    Text("Gekoppelte Geräte:", style = MaterialTheme.typography.titleLarge)
                }
                items(pairedDevices) { device ->
                    Button(onClick = {
                        selectedDeviceName = device
                        val deviceAddress = device.substringAfterLast("(").substringBefore(")")
                        selectedDevice = bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                        Toast.makeText(applicationContext, "Ausgewählt: $device", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(device)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gefundene Geräte:", style = MaterialTheme.typography.titleLarge)
                }
                items(scannedDevices.value) { device ->
                    Button(onClick = {
                        selectedDeviceName = device
                        val deviceAddress = device.substringAfterLast("(").substringBefore(")")
                        selectedDevice = bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                        Toast.makeText(applicationContext, "Ausgewählt: $device", Toast.LENGTH_SHORT).show()
                    }) {
                        Text(device)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(modifier = Modifier.size(64.dp).padding(4.dp), onClick = {
                    selectedDevice?.let {
                        connectToDevice(it)
                        startListeningForData(receivedData)
                    } ?: Toast.makeText(applicationContext, "Kein Gerät ausgewählt", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Verbinden", tint = Color.Blue)
                }
                IconButton(modifier = Modifier.size(64.dp).padding(4.dp), onClick = { startScanningDevices(scannedDevices) }) {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Scannen", tint = Color.Green)
                }
                IconButton(modifier = Modifier.size(64.dp).padding(4.dp), onClick = { sendHelloWorldToESP() }) {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = "Senden", tint = Color.Red)
                }
            }
        }
    }
}
