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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.*

class MainActivity : ComponentActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID für SPP
    private var selectedDevice: BluetoothDevice? = null
    private var outputStream: OutputStream? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BluetoothApp()
        }

        // Berechtigungen prüfen und anfordern
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
            ) {
                requestBluetoothPermissions.launch(
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        } else {
            Log.d("Bluetooth", "Berechtigungen nicht erforderlich für diese Android-Version.")
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
        var deviceList by remember { mutableStateOf(getPairedDevices()) }
        var selectedDeviceName by remember { mutableStateOf<String?>(null) }

        Column {
            Button(onClick = { sendHelloWorldToESP() }) {
                Text("Sende Nachricht")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Verbinden mit Gerät:")
            LazyColumn {
                items(deviceList) { device ->
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                selectedDevice?.let {
                    connectToDevice(it)
                } ?: Toast.makeText(applicationContext, "Kein Gerät ausgewählt", Toast.LENGTH_SHORT).show()
            }) {
                Text("Mit Gerät verbinden")
            }
        }
    }
}
