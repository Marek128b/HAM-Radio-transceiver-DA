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
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import at.htlklu.eintest.data.FunkyTempInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive


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

    private var stopScanHandler: Handler? = null // Handler für den Timer

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

            // Unregister Receiver sicherstellen
            bluetoothReceiver?.let {
                unregisterReceiver(it)
                bluetoothReceiver = null
            }

            // Stoppe den laufenden Handler, falls der Scan manuell gestoppt wurde
            stopScanHandler?.removeCallbacksAndMessages(null)
            stopScanHandler = null

        } else {
            try {
                if (bluetoothAdapter?.startDiscovery() == true) {
                    isScanning = true

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

                    registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))

                    // Erstelle und starte einen neuen Handler
                    stopScanHandler = Handler(Looper.getMainLooper())

                    // Nach 5 Sekunden den Scan automatisch stoppen
                    stopScanHandler?.postDelayed({
                        if (isScanning) {
                            bluetoothAdapter?.cancelDiscovery()
                            isScanning = false
                            Toast.makeText(this, "Scan automatisch gestoppt", Toast.LENGTH_SHORT).show()
                            bluetoothReceiver?.let {
                                unregisterReceiver(it)
                                bluetoothReceiver = null
                            }
                        }
                    }, 5000L)

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

    /*
    @Composable
    fun DevicePairingHandler(
        device: BluetoothDevice,
        navController: NavController
    ) {
        var isBonded = isDeviceBonded(device)
        var showPairingDialog by remember { mutableStateOf(!isBonded) }

        // Dialog anzeigen, wenn das Gerät noch nicht gekoppelt ist
        if (showPairingDialog) {
            AlertDialog(
                onDismissRequest = { showPairingDialog = false },
                title = { Text(text = "Gerät koppeln?") },
                text = { Text(text = "Möchtest du das Gerät ${device.name} koppeln?") },
                confirmButton = {
                    TextButton(onClick = {
                        showPairingDialog = false
                        // Gerät verbinden, wenn bestätigt
                        connectToDevice(device, navController)
                    }) {
                        Text("Ja")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPairingDialog = false }) {
                        Text("Nein")
                    }
                }
            )
        }
    }

    fun getBluetoothDeviceByAddress(address: String): BluetoothDevice? {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        return bluetoothAdapter?.getRemoteDevice(address)
    }

    fun isDeviceBonded(device: BluetoothDevice): Boolean {
        val pairedDevices = BluetoothAdapter.getDefaultAdapter()?.bondedDevices
        pairedDevices?.forEach {
            if (it.address == device.address) {
                return true  // Gerät ist gekoppelt
            }
        }
        return false  // Gerät ist nicht gekoppelt
    }
     */

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

            startSendingFunkyInfoPeriodically()

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
                stopFunkyInfoTask()
            } catch (closeException: IOException) {
                Log.e("Bluetooth", "Fehler beim Schließen des Sockets: ${closeException.message}")
            }
        }
    }

    fun disconnectFromDevice(bluetoothSocket: BluetoothSocket?, navController: NavController) {
        try {
            stopFunkyInfoTask()
            bluetoothSocket?.close()
            navController.navigate("bluetoothScreen")
            Log.d("Bluetooth", "Verbindung erfolgreich getrennt.")
        } catch (e: IOException) {
            Log.e("Bluetooth", "Fehler beim Trennen der Verbindung", e)
        }
    }

    private fun sendFunkyInfo(op: Boolean, onlyTemp: Boolean) {
        try {
            if (bluetoothSocket?.isConnected == true) {
                if (!onlyTemp) {
                    FunkyRepository.funkyInfo.op = op
                    val jsonString = Json.encodeToString(FunkyRepository.funkyInfo) + "\n"

                    outputStream?.write(jsonString.toByteArray(Charsets.UTF_8))
                    outputStream?.flush()

                    Log.d("Bluetooth", "JSON gesendet: $jsonString")
                }else{
                    var funkyTempInfo = FunkyTempInfo(op, FunkyRepository.funkyInfo.temperature)
                    val jsonString = Json.encodeToString(funkyTempInfo) + "\n"

                    outputStream?.write(jsonString.toByteArray(Charsets.UTF_8))
                    outputStream?.flush()

                    Log.d("Bluetooth", "JSON Temp gesendet: $jsonString")
                }
            } else {
                Toast.makeText(this, "Bluetooth ist nicht verbunden", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Bluetooth", "Fehler beim Senden der Nachricht: ${e.message}")
            Toast.makeText(this, "Fehler beim Senden: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun processIncomingJson(data: String){
        try {
            // Versuche, die Daten als FunkyInfo zu deserialisieren
            val parsedData = try {
                Json.decodeFromString<FunkyInfo>(data)
            } catch (e: Exception) {
                // Falls deserialisieren als FunkyInfo fehlschlägt, versuche es als FunkyTempInfo
                Log.e("FunkyInfo", "Fehler bei der Deserialisierung von FunkyInfo: ${e.message}")
                null
            }

            // Wenn es als FunkyInfo deserialisiert wurde, speichere es
            parsedData?.let {
                // Speichern von FunkyInfo in MainActivity.FunkyRepository
                if (!it.op) {
                    FunkyRepository.funkyInfo = it
                }
                Log.d("FunkyInfo", "FunkyInfo erfolgreich gespeichert: ${it}")
            } ?: run {
                // Wenn deserialisieren als FunkyInfo fehlschlug, versuche FunkyTempInfo
                val tempData = try {
                    Json.decodeFromString<FunkyTempInfo>(data)
                } catch (e: Exception) {
                    // Falls auch das fehlschlägt, logge den Fehler und breche ab
                    Log.e("FunkyTempInfo", "Fehler bei der Deserialisierung von FunkyTempInfo: ${e.message}")
                    return
                }

                // Speichern von FunkyTempInfo in MainActivity.FunkyRepository
                MainActivity.FunkyRepository.funkyInfo = FunkyInfo(
                    op = tempData.op,
                    frequency = MainActivity.FunkyRepository.funkyInfo.frequency,  // Setze Standardwerte
                    voltage = MainActivity.FunkyRepository.funkyInfo.voltage,
                    name = MainActivity.FunkyRepository.funkyInfo.name,
                    call = MainActivity.FunkyRepository.funkyInfo.call,
                    temperature = tempData.temperature
                )
                Log.d("FunkyTempInfo", "FunkyTempInfo erfolgreich gespeichert: ${tempData}")
            }
        } catch (e: Exception) {
            Log.e("PROCESSJSON", "Fehler bei der Verarbeitung der Daten: ${e.message}")
            sendFunkyInfo(false, false)
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
                                    processIncomingJson(fullData)

                                    // Nach erfolgreicher Verarbeitung den String zurücksetzen
                                    fullData = ""

                                } catch (e: Exception) {
                                    Log.e("Bluetooth", "Fehler beim Deserialisieren: ${e.message}")
                                }
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

    private var funkyInfoHandler: Handler? = null

    private fun startSendingFunkyInfoPeriodically() {
        if (funkyInfoHandler == null) {
            funkyInfoHandler = Handler(Looper.getMainLooper())
        }

        val funkyInfoRunnable = object : Runnable {
            override fun run() {
                if (bluetoothSocket?.isConnected == true) {
                    sendFunkyInfo(false, true)
                    Log.d("Bluetooth", "sendFunkyInfo(false) wurde aufgerufen.")
                }

                funkyInfoHandler?.postDelayed(this, 20000L)
            }
        }

        funkyInfoHandler?.post(funkyInfoRunnable)
    }

    private fun stopFunkyInfoTask() {
        funkyInfoHandler?.removeCallbacksAndMessages(null)
        funkyInfoHandler = null
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
                    BluetoothApp(receivedData, scannedDevices, navController)
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
                    .background(Color(0xAF000000))
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
                                            sendFunkyInfo(op = false, onlyTemp = false)
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
                                        modifier = Modifier.size(40.dp),
                                        tint = Color(0xFF2727FF)
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
                                        navController.navigate("bluetoothScreen")
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Trennen",
                                        modifier = Modifier.size(35.dp),
                                        tint = Color(0xFF2727FF)
                                    )
                                }
                            }
                        }
                    }

                    //----------------------------------------------------Scannen--------------------------------------------------
                    when (ObserveCurrentScreen(navController)) {
                        // Scannen-Button (Bluetooth Screen)
                        "bluetoothScreen" -> {
                            // Animierte Skalierung, die beim Drücken des Buttons stattfindet
                            val scale by animateFloatAsState(
                                targetValue = if (isScanning) 0.9f else 1f, // Skaliert den Button beim Drücken
                                animationSpec = tween(durationMillis = 150) // Dauer der Animation (150 ms)
                            )

                            Box(
                                modifier = Modifier.size(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .graphicsLayer(scaleX = scale, scaleY = scale), // Anwendung der Skalierung
                                    onClick = {
                                        // Startet das Scannen, nach dem Drücken
                                        startScanningDevices(scannedDevices)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Scannen",
                                        modifier = Modifier.size(40.dp),
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
                                        sendFunkyInfo(false, false)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh",
                                        modifier = Modifier.size(35.dp),
                                        tint = Color(0xFF2727FF)
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
                                sendFunkyInfo(true, false)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Send,
                                contentDescription = "Senden",
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF2727FF)
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
        scannedDevices: MutableState<List<String>>,
        navController: NavController
    ) {
        val pairedDevices by remember { mutableStateOf(getPairedDevices()) }
        var selectedDeviceName by remember { mutableStateOf<String?>(null) }

        var showPairingDialog by remember { mutableStateOf(false) }

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
                                .background(Color(0x21FFFFFF))
                                .padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xAF000000))
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
                                        var isPressed by remember { mutableStateOf(false) }
                                        val animatedColor by animateColorAsState(
                                            targetValue = if (isPressed) Color(0xFF17632E) else Color(0x1F4444FF)
                                        )
                                        val scale by animateFloatAsState(
                                            targetValue = if (isPressed) 0.95f else 1f
                                        )

                                        Box(
                                            modifier = Modifier
                                                .padding(10.dp, 0.dp, 10.dp, 10.dp)
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                }
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(animatedColor)
                                                .then(
                                                    if (selectedDeviceName == device)
                                                        Modifier.border(BorderStroke(3.dp, Color(0xFF199A40)), shape = RoundedCornerShape(10.dp))
                                                            .background(Color(0xFF17632E))
                                                    else Modifier
                                                )
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            isPressed = true
                                                            tryAwaitRelease()
                                                            isPressed = false
                                                            selectedDeviceName = device
                                                            val deviceAddress = device.substringAfterLast("(").substringBefore(")")
                                                            selectedDevice = bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                                                        }
                                                    )
                                                }
                                                .padding(20.dp)
                                        ) {
                                            // Geräteinformationen in zwei Zeilen: Gerätename und MAC-Adresse
                                            Column {
                                                val deviceName = device.substringBefore("(").trim()
                                                val macAddress = device.substringAfter("(").substringBefore(")").trim()
                                                Text(
                                                    text = deviceName,
                                                    color = Color.White,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = macAddress,
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
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
                                .background(Color(0x21FFFFFF))
                                .padding(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xAF000000))
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
                                        var isPressed by remember { mutableStateOf(false) }
                                        val animatedColor by animateColorAsState(
                                            targetValue = if (isPressed) Color(0xAF4444FF) else Color(0x1F4444FF)
                                        )
                                        val scale by animateFloatAsState(
                                            targetValue = if (isPressed) 0.95f else 1f
                                        )

                                        Box(
                                            modifier = Modifier
                                                .padding(10.dp, 0.dp, 10.dp, 10.dp)
                                                .fillMaxWidth()
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                }
                                                .clip(RoundedCornerShape(10.dp)) // Abgerundete Ecken
                                                .background(animatedColor) // Animierter Hintergrund
                                                .then(
                                                    if (selectedDeviceName == device)
                                                        Modifier.border(BorderStroke(3.dp, Color(0xFF199A40)), shape = RoundedCornerShape(10.dp))
                                                            .background(Color(0xFF17632E))
                                                    else Modifier
                                                )
                                                .pointerInput(Unit) {
                                                    detectTapGestures(
                                                        onPress = {
                                                            // Animation starten
                                                            isPressed = true
                                                            // Warten bis der Finger losgelassen wird
                                                            tryAwaitRelease()
                                                            isPressed = false
                                                            // Klick-Logik ausführen
                                                            selectedDeviceName = device
                                                            val deviceAddress = device.substringAfterLast("(").substringBefore(")")
                                                            selectedDevice = bluetoothAdapter?.bondedDevices?.find { it.address == deviceAddress }
                                                            val bluetoothDevice = BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(
                                                                device.substringAfter("(").substringBefore(")").trim()
                                                            )

                                                            showPairingDialog = true
                                                            Log.d("SHOWPAIRING", showPairingDialog.toString())

                                                        }
                                                    )
                                                }
                                                .padding(20.dp) // Inneres Padding
                                        ) {
                                            Column {
                                                val deviceName = device.substringBefore("(").trim()
                                                val macAddress = device.substringAfter("(").substringBefore(")").trim()
                                                Text(
                                                    text = deviceName,
                                                    color = Color.White,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = macAddress,
                                                    color = Color.White,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    Log.d("DEVICE", selectedDeviceName.toString())

                                    /*
                                    if (showPairingDialog) {
                                        Log.d("DEVICEPAIRING", showPairingDialog.toString())
                                        var device = getBluetoothDeviceByAddress(
                                            selectedDeviceName.toString().substringAfter("(").substringBefore(")").trim()
                                        )
                                        if (device != null) {
                                            DevicePairingHandler(device = device, navController = navController)
                                        }

                                    }
                                    */

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