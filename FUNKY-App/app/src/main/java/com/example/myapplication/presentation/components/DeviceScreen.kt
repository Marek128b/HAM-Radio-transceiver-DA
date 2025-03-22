package com.example.myapplication.presentation.components

import android.content.ClipData.Item
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.chat.BluetoothDevice
import com.example.myapplication.presentation.BluetoothUiState
import dagger.Lazy
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@Composable
fun DeviceScreen(
    state: BluetoothUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onStartServer: () -> Unit,
    onDeviceClick: (BluetoothDevice) -> Unit
) {

    var buttonState by remember { mutableStateOf(true) }
    var buttonColorState by remember { mutableStateOf(Color(0xFF1F8B2F)) }

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF0F2459), Color(0xFF4B42CC)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1500f, 1500f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            //Screen
            BluetoothDeviceList(
                pairedDevices = state.pairedDevices,
                scannedDevices = state.scannedDevices,
                onClick = onDeviceClick,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )

        }

        //Buttons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(buttonColorState, shape = RoundedCornerShape(100))
                    .clickable {
                        if (buttonState) {
                            onStartScan()
                            buttonState = false
                            buttonColorState = Color(0xFF9F2E30)
                        } else {
                            onStopScan()
                            buttonState = true
                            buttonColorState = Color(0xFF1F8B2F)
                        }
                    }
            ) {

                Icon(
                    Icons.Rounded.Search,
                    null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp, 20.dp, 30.dp, 30.dp)
                        .graphicsLayer { scaleX = -1f },
                    tint = Color.White
                )
                Icon(
                    Icons.Rounded.PlayArrow,
                    null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp, 40.dp, 10.dp, 10.dp),
                    tint = Color.White
                )
            }

            Button(onClick = onStartServer) {
                Text(text = "Start server")
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    // Eine LazyColumn, die alles enthält und scrollen kann
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp, 20.dp, 0.dp)
    ) {
        // Spacer für Abstand zwischen den Boxen
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ------------------------------------------------------- Paired Devices Abschnitt -------------------------------------------------------

        item   {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(10.dp),
            ) {
                Column {

                    //Paired Devices anzeige
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text = "Paired Devices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp),
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

                    //bluetooh paired devices
                    pairedDevices.forEach { device ->
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x0AFFFFFF))
                                .clickable { onClick(device) }
                        ) {
                            Column {
                                Text(
                                    text = device.name ?: "(No name)",
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp, 10.dp, 16.dp, 0.dp)
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp, 0.dp, 16.dp, 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Spacer für Abstand zwischen den Boxen
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }

        // ------------------------------------------------------- Scanned Devices Abschnitt -------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(10.dp)
            ) {
                Column {

                    //Scanned Devices anzeige
                    Box(modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text = "Scanned Devices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(20.dp, 0.dp, 20.dp, 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF9A1919))
                            .height(5.dp)
                    )

                    scannedDevices.forEach { device ->
                        if (device.name != null) {
                            Box(
                                modifier = Modifier
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color(0x0AFFFFFF))
                                    .clickable { onClick(device) }
                            ) {
                                Column {
                                    Text(
                                        text = device.name ?: "(No name)",
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp, 10.dp, 16.dp, 0.dp)
                                    )
                                    Text(
                                        text = device.address,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp, 0.dp, 16.dp, 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Spacer für Abstand zwischen den Boxen
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


/*
@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDevice>,
    scannedDevices: List<BluetoothDevice>,
    onClick: (BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1AFFFFFF))
        ) {
            Column {
                Text(
                    text = "Paired Devices",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )

                LazyColumn (userScrollEnabled = false){
                    items(pairedDevices) { device ->
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x0AFFFFFF))
                        ) {
                            Text(
                                text = device.name ?: "(No name)",
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick(device) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }


        Box(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1AFFFFFF))
        ) {
            Column {
                Text(
                    text = "Scanned Devices",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(16.dp),
                    color = Color.White
                )

                LazyColumn {
                    items(scannedDevices) { device ->
                        Box(
                            modifier = Modifier
                                .padding(5.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0x0AFFFFFF))
                        ) {
                            Text(
                                text = device.name ?: "(No name)",
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onClick(device) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    /*

    Column {
        //------------------------------Paired Devices-------------------------------------------
        Box(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1AFFFFFF))

        ) {
            LazyColumn(
                modifier = modifier.padding(10.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Paired Devices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
                        )
                    }
                }

                items(pairedDevices) { device ->
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0x0AFFFFFF))
                    ) {
                        Text(
                            text = device.name ?: "(No name)",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClick(device) }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        //------------------------------Paired Devices-------------------------------------------
        Box(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x1AFFFFFF))
        ) {
            LazyColumn(
                modifier = modifier.padding(10.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scanned Devices",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(16.dp),
                            color = Color.White
                        )
                    }
                }

                items(scannedDevices) { device ->
                    Box(
                        modifier = Modifier
                            .padding(5.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0x0AFFFFFF))
                    ) {
                        Text(
                            text = device.name ?: "(No name)",
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClick(device) }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }

     */
}

 */