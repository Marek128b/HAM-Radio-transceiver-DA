package at.htlklu.eintest;


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard UUID für SPP
    private var selectedDevice: BluetoothDevice? = null
    private var outputStream: OutputStream? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Button-Referenz
        val sendButton: Button = findViewById(R.id.sendButton)

        // Setze OnClickListener für den Button
        sendButton.setOnClickListener {
            sendHelloWorldToESP()
        }

        // Überprüfen, ob die Berechtigung für Bluetooth_CONNECT gewährt wurde
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // Falls nicht, fordere die Berechtigung an
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }

        val selectDeviceButton: Button = findViewById(R.id.selectDeviceButton)
        val connectButton: Button = findViewById(R.id.connectButton)
        val listView: ListView = findViewById(R.id.deviceListView)

        // Gepaarten Geräte anzeigen
        selectDeviceButton.setOnClickListener {
            showPairedDevices(listView)
        }

        // Verbindung herstellen
        connectButton.setOnClickListener {
            selectedDevice?.let {
                connectToDevice(it)
            } ?: Toast.makeText(this, "Kein Gerät ausgewählt", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Berechtigung wurde gewährt, du kannst jetzt mit Bluetooth arbeiten
                Log.d("Bluetooth", "Berechtigung gewährt. Bluetooth kann verwendet werden.")
            } else {
                // Berechtigung verweigert
                Log.d("Bluetooth", "Berechtigung verweigert. Bluetooth kann nicht verwendet werden.")
                Toast.makeText(this, "Bluetooth-Berechtigung erforderlich", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPairedDevices(listView: ListView) {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        val deviceList = mutableListOf<String>()
        val devicesMap = mutableMapOf<String, BluetoothDevice>()

        pairedDevices?.forEach { device ->
            deviceList.add("${device.name} (${device.address})")
            devicesMap["${device.name} (${device.address})"] = device
        }

        if (deviceList.isNotEmpty()) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceList)
            listView.adapter = adapter

            // Gerät auswählen, wenn darauf geklickt wird
            listView.setOnItemClickListener { _, _, position, _ ->
                val deviceInfo = deviceList[position]
                selectedDevice = devicesMap[deviceInfo]
                Toast.makeText(this, "Ausgewählt: $deviceInfo", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Keine gepaarten Geräte gefunden", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            bluetoothSocket = device.createRfcommSocketToServiceRecord(deviceUUID)
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            Toast.makeText(this, "Verbindung zu ${device.name} hergestellt!", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Verbindungsfehler: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendHelloWorldToESP() {
        selectedDevice?.let {
            try {
                // Stelle sicher, dass Bluetooth verbunden ist
                if (bluetoothSocket?.isConnected == true) {
                    outputStream = bluetoothSocket?.outputStream
                    val message = "ENDLIIIIIIIIIIIIIIIIIIIIIIIIIIICHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH"
                    outputStream?.write(message.toByteArray())
                    Toast.makeText(this, "Nachricht gesendet: $message", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Bluetooth ist nicht verbunden", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Fehler beim Senden der Nachricht: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: Toast.makeText(this, "Kein Gerät ausgewählt oder verbunden", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothSocket?.close() // Schließe die Verbindung beim Beenden der App
    }
}