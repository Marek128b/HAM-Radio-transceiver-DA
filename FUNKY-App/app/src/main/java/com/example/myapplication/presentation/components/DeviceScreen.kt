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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF0F2459), Color(0xFFB7145A)),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = onStartScan) {
                    Text(text = "Start scan")
                }
                Button(onClick = onStopScan) {
                    Text(text = "Stop scan")
                }
                Button(onClick = onStartServer) {
                    Text(text = "Start server")
                }
            }
            BluetoothDeviceList(
                pairedDevices = state.pairedDevices,
                scannedDevices = state.scannedDevices,
                onClick = onDeviceClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

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
            .padding(20.dp)
    ) {
        // Paired Devices Abschnitt
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Paired Devices",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )

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
                                        .padding(16.dp,10.dp,16.dp,0.dp)
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp,0.dp,16.dp,10.dp)
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

        // Scanned Devices Abschnitt
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Scanned Devices",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )

                    scannedDevices.forEach { device ->
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