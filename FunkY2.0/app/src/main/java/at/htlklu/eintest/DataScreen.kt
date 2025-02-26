package at.htlklu.eintest.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import at.htlklu.eintest.MainActivity
import at.htlklu.eintest.data.FunkyInfo
import java.util.Locale
import kotlinx.serialization.json.Json

@Composable
fun DataScreen(navController: NavController, receivedData: String) {

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF11144F), Color(0xFF1F4596)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1500f, 1500f)
    )


    if (receivedData.isNotEmpty()) {
        try {
            // JSON-Daten in FunkyInfo umwandeln
            val parsedFunkyInfo = Json.decodeFromString<FunkyInfo>(receivedData)

            // Neuen FunkyInfo-Wert mit der formatierten Frequenz speichern
            MainActivity.FunkyRepository.funkyInfo = parsedFunkyInfo

            Log.d(
                "DataScreen",
                "Erfolgreich deserialisiert: ${MainActivity.FunkyRepository.funkyInfo}"
            )
        } catch (e: Exception) {
            Log.e("DataScreen", "Fehler bei der Deserialisierung der Daten: ${e.message}")
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val spacing = 16.dp
            val squareSize = (maxWidth - spacing) / 2

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                // Obere Zeile mit 2 Quadraten
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    // Box für Name & Call
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(squareSize)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0x11FFFFFF))
                            .padding(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x050000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = MainActivity.FunkyRepository.funkyInfo.name,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = MainActivity.FunkyRepository.funkyInfo.call,
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    // Box für Voltage
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(squareSize)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0x11FFFFFF))
                            .padding(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x050000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = MainActivity.FunkyRepository.funkyInfo.voltage.toString(),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(squareSize)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0x11FFFFFF))
                        .padding(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x050000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        FrequencyLock(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }


                // Box für Temperatur
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(squareSize)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0x11FFFFFF))
                        .padding(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x050000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = MainActivity.FunkyRepository.funkyInfo.temperature.toString(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun FrequencyLock(
    modifier: Modifier = Modifier
) {
    var currentFrequency by remember { mutableStateOf(MainActivity.FunkyRepository.funkyInfo.frequency) }

    Log.d("DEBUG", "$currentFrequency")

    val formattedFrequency = String.format(Locale.US, "%.4f", currentFrequency) // Immer 4 Nachkommastellen
    val parts = formattedFrequency.split(".") // Trennen in Vorkomma- und Nachkommazahlen

// Stelle sicher, dass die Nachkommastellen genau 4 Zeichen lang sind
    val decimalPart = parts.getOrNull(1)?.padEnd(4, '0') ?: "0000" // Falls kürzer, mit Nullen auffüllen

    val integerPart = parts[0].map { it.toString().toInt() }
    var decimalDigits by remember { mutableStateOf(decimalPart.map { it.toString().toInt() }) }

    Log.d("DEBUG", "Formatted Frequency: $formattedFrequency")
    Log.d("DEBUG", "Decimal Part: $decimalPart")


    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        integerPart.forEach { digit ->
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = digit.toString(),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
            Text(
                text = ",",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }

        decimalDigits.forEachIndexed { index, digit ->
            if (index == 3) {
                Box(modifier = Modifier.width(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = ".",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            NumberWheel(
                value = digit,
                onValueChange = { newDigit ->
                    val newDigits = decimalDigits.toMutableList().apply { this[index] = newDigit }
                    decimalDigits = newDigits

                    val newFrequencyString = "${integerPart.joinToString("")}.${newDigits.joinToString("")}"
                    val newFrequency = newFrequencyString.toFloat()

                    // **Direkt speichern in FunkyRepository und UI updaten**
                    MainActivity.FunkyRepository.funkyInfo.frequency = newFrequency
                    currentFrequency = newFrequency  // **UI-Refresh sicherstellen**
                },
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

@Composable
fun NumberWheel(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedValue by remember { mutableStateOf(value) }

    LaunchedEffect(value) {
        displayedValue = value // **Sorgt dafür, dass NumberWheel immer den aktuellen Wert anzeigt**
    }

    Column(
        modifier = modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = {
            val newValue = if (displayedValue == 9) 0 else displayedValue + 1
            displayedValue = newValue
            onValueChange(newValue) // **Sofort speichern**
        }) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = Color.White
            )
        }
        Text(
            text = displayedValue.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = {
            val newValue = if (displayedValue == 0) 9 else displayedValue - 1
            displayedValue = newValue
            onValueChange(newValue) // **Sofort speichern**
        }) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = Color.White
            )
        }
    }
}



//14,000 000 MHz - 14,350 000 MHz in 100 Hz schritten
