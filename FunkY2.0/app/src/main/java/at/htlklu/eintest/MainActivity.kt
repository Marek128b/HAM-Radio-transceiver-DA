package at.htlklu.eintest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.htlklu.eintest.data.FunkyInfo
import at.htlklu.eintest.ui.DataScreen
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map


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
            AppNavigation()
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

    private fun connectToDevice(device: BluetoothDevice, navController: NavController) {
        try {
            // Erstelle eine neue Verbindung, wenn keine bestehende Verbindung mehr besteht
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(deviceUUID)
            bluetoothAdapter?.cancelDiscovery()  // Vermeide Verbindungsprobleme
            bluetoothSocket?.connect()  // Baue die Verbindung auf
            outputStream = bluetoothSocket?.outputStream

            // Bestätigung der erfolgreichen Verbindung
            Toast.makeText(
                applicationContext,
                "Verbindung zu ${device.name} hergestellt!",
                Toast.LENGTH_SHORT
            ).show()

            // Navigiere zum DataScreen
            navController.navigate("dataScreen")

        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Verbindungsfehler: ${e.message}")
            Toast.makeText(
                applicationContext,
                "Verbindungsfehler: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()

            // Falls ein Fehler beim Schließen oder Verbinden auftritt, versuche den Socket zu schließen
            try {
                bluetoothSocket?.close()
            } catch (closeException: IOException) {
                Log.e("Bluetooth", "Fehler beim Schließen des Sockets: ${closeException.message}")
            }
        }
    }

    fun disconnectFromDevice(bluetoothSocket: BluetoothSocket?, navController: NavController) {
        try {
            bluetoothSocket?.close()
            navController.navigate("bluetoothScreen")
            Log.d("Bluetooth", "Verbindung erfolgreich getrennt.")
        } catch (e: IOException) {
            Log.e("Bluetooth", "Fehler beim Trennen der Verbindung", e)
        }
    }

    private fun connectToDeviceFromScannedList(deviceInfo: String) {
        try {
            // Extrahiere die Geräteadresse aus dem String (z. B. "DeviceName (00:11:22:33:44:55)")
            val deviceAddress = deviceInfo.substringAfterLast("(").substringBefore(")")
            val device = bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                ?: bluetoothAdapter?.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Bluetooth", "Fehler bei Verbindung zu $deviceInfo: ${e.message}")
        }
    }

    private fun sendFunkyInfo(op: Boolean) {
        try {
            if (bluetoothSocket?.isConnected == true) {
                FunkyRepository.funkyInfo.op = op
                val jsonString = Json.encodeToString(FunkyRepository.funkyInfo) + "\n"

                outputStream?.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream?.flush()

                Log.d("Bluetooth", "JSON gesendet: $jsonString")
                //Toast.makeText(this, "Nachricht gesendet: $jsonString", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Bluetooth ist nicht verbunden", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Fehler beim Senden der Nachricht: ${e.message}")
            Toast.makeText(this, "Fehler beim Senden: ${e.message}", Toast.LENGTH_SHORT).show()
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
                var fullData = "" // String, der alle empfangenen Daten speichert

                while (bluetoothSocket?.isConnected == true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead).trim()

                        if (data.isNotEmpty()) {
                            Log.d("Bluetooth", "Empfangene Daten: $data")
                            fullData += data // Daten an bereits empfangene anhängen

                            // Prüfen, ob der JSON-String vollständig und gültig ist
                            if (fullData.startsWith("{") && fullData.endsWith("}")) {
                                try {
                                    val parsedFunkyInfo = Json.decodeFromString<FunkyInfo>(fullData)
                                    if(parsedFunkyInfo.op){
                                        Log.d("FEHLER", "Fehler")
                                        sendFunkyInfo(true)
                                    }else {
                                        // Frequenz auf genau 4 Nachkommastellen formatieren
                                        val formattedFrequency = String.format(
                                            Locale.US,
                                            "%.4f",
                                            parsedFunkyInfo.frequency
                                        ).toFloat()

                                        // Daten sofort in FunkyRepository speichern
                                        MainActivity.FunkyRepository.funkyInfo =
                                            parsedFunkyInfo.copy(frequency = formattedFrequency)

                                        Log.d(
                                            "Bluetooth",
                                            "Erfolgreich gespeichert: ${MainActivity.FunkyRepository.funkyInfo}"
                                        )

                                        // UI-Update auf dem Main-Thread
                                        withContext(Dispatchers.Main) {
                                            receivedData.value = fullData
                                        }
                                    }

                                    // Nach erfolgreicher Verarbeitung den String zurücksetzen
                                    fullData = ""

                                } catch (e: Exception) {
                                    Log.e("Bluetooth", "Fehler beim Deserialisieren: ${e.message}")
                                }
                            }
                            //Erneutes senden

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

    object FunkyRepository {
        var funkyInfo by mutableStateOf(
            FunkyInfo(
                false,
                14f,
                10f,
                "Default-Name",
                "Default-Call",
                15f
            )
        )
    }

    @Composable
    fun ObserveCurrentScreen(navController: NavController): String? {
        val currentRoute by navController.currentBackStackEntryFlow
            .map { it?.destination?.route }
            .collectAsState(initial = "home") // Standard-Route setzen, falls nichts vorhanden ist

        return currentRoute
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val receivedData = remember { mutableStateOf("") }
        val scannedDevices = remember { mutableStateOf(listOf<String>()) }

        Box(modifier = Modifier.fillMaxSize()) {
            // NavHost für die Navigation zwischen den Screens
            NavHost(navController = navController, startDestination = "bluetoothScreen") {
                composable("bluetoothScreen") {
                    BluetoothApp(receivedData, scannedDevices)
                }
                composable("dataScreen") {
                    DataScreen(navController) // Übergeben der Daten
                }
            }

            // Hotbar bleibt immer am unteren Rand sichtbar
            Hotbar(navController, scannedDevices, receivedData)
        }
    }

    @Composable
    fun Hotbar(
        navController: NavController,
        scannedDevices: MutableState<List<String>>,
        receivedData: MutableState<String>,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xAFFFFFFF))
                    .clickable(enabled = true, onClick = {}) // Verhindert Durchklicken
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //------------------------------------------Verbinden-----------------------------------------
                    when (ObserveCurrentScreen(navController)) {
                        // Verbinden-Button (Bluetooth Screen)
                        "bluetoothScreen" -> {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    modifier = Modifier.size(64.dp),
                                    onClick = {
                                        selectedDevice?.let {
                                            connectToDevice(it, navController)
                                            startListeningForData(receivedData)
                                            sendFunkyInfo(false)
                                        } ?: Toast.makeText(
                                            applicationContext,
                                            "Kein Gerät ausgewählt",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Verbinden",
                                        modifier = Modifier.size(35.dp),
                                        tint = Color.Blue
                                    )
                                }
                            }
                        }

                        // Disconnect-Button (Data Screen)
                        "dataScreen" -> {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    modifier = Modifier.size(64.dp),
                                    onClick = {
                                        selectedDevice?.let {
                                            disconnectFromDevice(bluetoothSocket, navController)
                                        } ?: Toast.makeText(
                                            applicationContext,
                                            "Kein Gerät ausgewählt",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Trennen",
                                        modifier = Modifier.size(35.dp),
                                        tint = Color.Blue
                                    )
                                }
                            }
                        }
                    }

                    //----------------------------------------------------Scannen--------------------------------------------------
                    when (ObserveCurrentScreen(navController)) {
                        // Scannen-Button (Bluetooth Screen)
                        "bluetoothScreen" -> {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    modifier = Modifier.size(64.dp),
                                    onClick = { startScanningDevices(scannedDevices) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Scannen",
                                        modifier = Modifier.size(35.dp),
                                        tint = if (isScanning) Color.Red else Color(0xFF199A40)
                                    )
                                }
                            }
                        }

                        // Refresh-Button (Data Screen)
                        "dataScreen" -> {
                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    modifier = Modifier.size(64.dp),
                                    onClick = {
                                        sendFunkyInfo(false)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(35.dp),
                                        tint = Color.Blue
                                    )
                                }
                            }
                        }
                    }

                    //------------------------------------------------------------Senden-----------------------------------------------------
                    Box(
                        modifier = Modifier.size(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            modifier = Modifier.size(64.dp),
                            onClick = {
                                sendFunkyInfo(true)
                            }
                        ) {
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


    @Composable
    fun BluetoothApp(
        receivedData: MutableState<String>,
        scannedDevices: MutableState<List<String>>
    ) {
        val pairedDevices by remember { mutableStateOf(getPairedDevices()) }
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
        }
    }
}