package at.htlklu.eintest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import androidx.compose.ui.zIndex

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



    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                // Berechtigung wurde erteilt
                Toast.makeText(this, "Standortberechtigung erteilt", Toast.LENGTH_SHORT).show()
            } else {
                // Berechtigung wurde verweigert
                Toast.makeText(this, "Standortberechtigung verweigert", Toast.LENGTH_SHORT).show()
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
        requestPermissionsTest()
        requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)

    }

    private fun checkAndRequestBluetoothPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
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

    private fun requestPermissionsTest() {
        requestBluetoothPermissions.launch(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        )
    }



    private var bluetoothReceiver: BroadcastReceiver? = null

    private fun startScanningDevices(scannedDevices: MutableState<List<String>>) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(
                this,
                "Bluetooth ist deaktiviert oder nicht verfügbar",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isScanning) {
            bluetoothAdapter?.cancelDiscovery()
            isScanning = false
            Toast.makeText(this, "Scan gestoppt", Toast.LENGTH_SHORT).show()

            // Unregister Receiver sicherstellen
            bluetoothReceiver?.let {
                unregisterReceiver(it)
                bluetoothReceiver = null
            }
        } else {
            try {
                if (bluetoothAdapter?.startDiscovery() == true) {
                    isScanning = true
                    Toast.makeText(this, "Scan gestartet", Toast.LENGTH_SHORT).show()

                    bluetoothReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                                val device =
                                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                device?.let {
                                    if (!it.name.isNullOrEmpty() && !scannedDevices.value.contains("${it.name} (${it.address})")) {
                                        scannedDevices.value =
                                            scannedDevices.value + "${it.name} (${it.address})"
                                        Log.d("Bluetooth", "Gefunden: ${it.name} (${it.address})")
                                    }
                                }
                            }
                        }
                    }

                    // Registriere den Receiver
                    registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
                } else {
                    Log.e("Bluetooth", "startDiscovery() fehlgeschlagen.")
                    Toast.makeText(this, "Scan konnte nicht gestartet werden", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Log.e("Bluetooth", "Fehler beim Starten des Scans: ${e.message}")
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

    private fun connectToDeviceFromScannedList(deviceInfo: String) {
        try {
            // Extrahiere die Geräteadresse aus dem String (z. B. "DeviceName (00:11:22:33:44:55)")
            val deviceAddress = deviceInfo.substringAfterLast("(").substringBefore(")")
            val device = bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                ?: bluetoothAdapter?.getRemoteDevice(deviceAddress)

            if (device != null) {
                connectToDevice(device)
            } else {
                Toast.makeText(this, "Gerät nicht gefunden: $deviceInfo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Bluetooth", "Fehler bei Verbindung zu $deviceInfo: ${e.message}")
        }
    }



    private fun sendHelloWorldToESP() {
        try {
            if (bluetoothSocket?.isConnected == true) {
                val message = "Halle hier ist FunkY App"
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

        Box(
            Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {

            Column(modifier = Modifier.fillMaxSize()) {
                // Überschrift
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

                // LazyColumn zur Anzeige der empfangenen Daten
                LazyColumn(
                    modifier = Modifier
                        .weight(1f) // Verteilt den verfügbaren Platz
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(2f)
                        .padding(20.dp, 0.dp)
                ) {

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }


                    // -------------------------------------Paired Deviecs------------------------------------------
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color(0x11FFFFFF))
                                .padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x050000000))
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
                                                .background(Color(0x1F4444FF)) // Hintergrund mit abgerundeten Ecken
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
                                            Text(device, color = Color.White, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }


                    //Spacer
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // -------------------------------------Scanned Deviecs------------------------------------------
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(25.dp))
                                .background(Color(0x11FFFFFF))
                                .padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0x050000000))
                            ) {
                                Column {

                                    //Scanned Devices anzeige
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Scanned Devices",
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
                                    scannedDevices.value.forEach { device ->
                                        Box(
                                            modifier = Modifier
                                                .padding(10.dp, 0.dp, 10.dp, 10.dp)
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0x1F4444FF))
                                                //.background(Color(0x2F0000FF))
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
                                                    connectToDeviceFromScannedList(device)
                                                    startListeningForData(receivedData)
                                                })
                                                .padding(20.dp) // Padding innerhalb der abgerundeten Box
                                        )

                                        {
                                            Text(device, color = Color.White, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }


                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }

                }
            }


            // Hotbar bleibt unabhängig und unten sichtbar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xAFFFFFFF))
                    .zIndex(1f) // Hotbar bleibt über den anderen Elementen
            ) {
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
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Verbinden",
                            modifier = Modifier.size(35.dp),
                            tint = Color.Blue
                        )
                    }
                    IconButton(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp),
                        onClick = { startScanningDevices(scannedDevices) }) {
                        if (isScanning){
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Scannen",
                                modifier = Modifier.size(35.dp),
                                tint = Color.Red
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Scannen",
                                modifier = Modifier.size(35.dp),
                                tint = Color(0xFF199A40)
                            )
                        }

                    }
                    IconButton(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp),
                        onClick = { sendHelloWorldToESP() }) {
                        Icon(
                            imageVector = Icons.Rounded.Send,
                            contentDescription = "Senden",
                            modifier = Modifier.size(35.dp),
                            tint = Color.Blue
                        )
                    }
                }
            }




        }
    }
}