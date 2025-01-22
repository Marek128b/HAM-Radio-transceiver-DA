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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    private val deviceUUID: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID für SPP
    private var selectedDevice: BluetoothDevice? = null
    private var outputStream: OutputStream? = null
    private var isScanning by mutableStateOf(false)

    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("Bluetooth", "${it.key} = ${it.value}")
            }
            if (permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[android.Manifest.permission.BLUETOOTH_SCAN] == true
            ) {
                Log.d("Bluetooth", "Berechtigungen gewährt. Bluetooth kann verwendet werden.")
            } else {
                Log.d(
                    "Bluetooth",
                    "Berechtigungen verweigert. Bluetooth kann nicht verwendet werden."
                )
                Toast.makeText(this, "Bluetooth-Berechtigungen erforderlich", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
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
                        override fun onReceive(
                            context: android.content.Context?,
                            intent: android.content.Intent?
                        ) {
                            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                                val device =
                                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                device?.let {
                                    if (!it.name.isNullOrEmpty()) { // Nur Geräte mit Namen hinzufügen
                                        scannedDevices.value =
                                            scannedDevices.value + "${it.name} (${it.address})"
                                    }
                                }
                            }
                        }
                    },
                    android.content.IntentFilter(BluetoothDevice.ACTION_FOUND)
                )
            } else {
                Toast.makeText(this, "Scan konnte nicht gestartet werden", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(deviceUUID)
            bluetoothAdapter?.cancelDiscovery() // Vermeide Verbindungsprobleme
            bluetoothSocket?.connect()  // Verbinde
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(
                applicationContext,
                "Verbindung zu ${device.name} hergestellt!",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Verbindungsfehler: ${e.message}")
            Toast.makeText(
                applicationContext,
                "Verbindungsfehler: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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
            Toast.makeText(
                this,
                "Fehler beim Senden der Nachricht: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startListeningForData(receivedData: MutableState<List<String>>) {
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
                        val data = String(buffer, 0, bytesRead).trim()
                        if (data.isNotEmpty()) {
                            Log.d("Bluetooth", "Empfangene Daten: $data")
                            withContext(Dispatchers.Main) {
                                // Neue Daten an die Liste anhängen
                                receivedData.value = receivedData.value + data
                            }
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
        val receivedData = remember { mutableStateOf(listOf<String>()) }
        var selectedDeviceName by remember { mutableStateOf<String?>(null) }

        val gradientBrush = Brush.linearGradient(
            colors = listOf(Color(0xFF11144F), Color(0xFF1F4596)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(1500f, 1500f)
        )

        val screenWidth = LocalConfiguration.current.screenWidthDp
        val screenHeight = LocalConfiguration.current.screenHeightDp

        val radialGradientBrush = Brush.radialGradient(
            0f to Color.Red,
            100.0f to Color.Green,
            center = Offset(
                screenWidth / 2f, screenHeight / 2f
            ),
            radius = 1000.0f,
            tileMode = TileMode.Clamp
        )


        Box(
            Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                /*
                Text(
                    text = "Empfangene Daten:",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
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
                    items(receivedData.value.reversed()) { data ->
                        Text(
                            text = data,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                 */

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp, 0.dp)
                ) {

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // Die große Box, die alle kleinen Boxen umhüllt
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color.White)
                                .padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(gradientBrush)
                            ) {
                                Column {

                                    //Paired Devices anzeige
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Paired Devices",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            color = Color.White
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .padding(20.dp, 0.dp, 20.dp, 20.dp)
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF199A40))
                                            .height(5.dp)
                                    )
                                    pairedDevices.forEach { device ->
                                        Box(
                                            modifier = Modifier
                                                .padding(10.dp, 0.dp, 10.dp, 10.dp)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp)) // Clip kommt hier, um abgerundete Ecken auf die Box anzuwenden
                                                .background(Color(0x2F0000FF)) // Hintergrund mit abgerundeten Ecken
                                                .clickable(onClick = {
                                                    selectedDeviceName = device
                                                    val deviceAddress =
                                                        device.substringAfterLast("(")
                                                            .substringBefore(")")
                                                    selectedDevice =
                                                        bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Ausgewählt: $device",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                })
                                                .padding(20.dp) // Padding innerhalb der abgerundeten Box
                                        )

                                        {
                                            Text(device, color = Color.White)
                                        }
                                    }
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
                            IconButton(modifier = Modifier
                                .size(64.dp)
                                .padding(4.dp), onClick = {
                                selectedDevice?.let {
                                    connectToDevice(it)
                                    startListeningForData(receivedData)
                                } ?: Toast.makeText(
                                    applicationContext,
                                    "Kein Gerät ausgewählt",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Verbinden",
                                    tint = Color.Blue
                                )
                            }
                            IconButton(
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(4.dp),
                                onClick = { startScanningDevices(scannedDevices) }) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Scannen",
                                    tint = Color.Green
                                )
                            }
                            IconButton(
                                modifier = Modifier
                                    .size(64.dp)
                                    .padding(4.dp),
                                onClick = { sendHelloWorldToESP() }) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Senden",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}