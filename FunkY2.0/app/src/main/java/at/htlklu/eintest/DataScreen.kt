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
import kotlinx.serialization.decodeFromString
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
            MainActivity.FunkyRepository.funkyInfo = Json.decodeFromString<FunkyInfo>(receivedData)
            Log.d("DataScreen", "Erfolgreich deserialisiert: ${MainActivity.FunkyRepository.funkyInfo}")
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

                // Box für Frequenz – hier wird das FrequencyLock (Zahlenrad) verwendet
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
                            frequency = MainActivity.FunkyRepository.funkyInfo.frequency,
                            onFrequencyChange = { newFreq ->
                                MainActivity.FunkyRepository.funkyInfo =
                                    MainActivity.FunkyRepository.funkyInfo.copy(frequency = newFreq)
                            },
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



// NumberWheel zeigt einen einzelnen Ziffernwert an, den du über die Pfeile ändern kannst.
@Composable
fun NumberWheel(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = {
            // Bei 9 wieder auf 0 setzen, ansonsten +1
            val newValue = if (value == 9) 0 else value + 1
            onValueChange(newValue)
        }) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = Color.White
            )
        }
        Text(
            text = value.toString(),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = {
            // Bei 0 zu 9 setzen, ansonsten -1
            val newValue = if (value == 0) 9 else value - 1
            onValueChange(newValue)
        }) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = Color.White
            )
        }
    }
}

// FrequencyLock teilt den Frequenzwert in seine einzelnen Ziffern auf und zeigt für jede ein NumberWheel an.
@Composable
fun FrequencyLock(
    frequency: Float,
    onFrequencyChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Frequenz als ganzzahligen Wert und als 4-stelligen String (ggf. mit führenden Nullen)
    val freqInt = frequency.toInt()
    val freqString = freqInt.toString().padStart(4, '0')
    var digits by remember { mutableStateOf(freqString.map { it.toString().toInt() }) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        digits.forEachIndexed { index, digit ->
            NumberWheel(
                value = digit,
                onValueChange = { newDigit ->
                    val newDigits = digits.toMutableList().apply { this[index] = newDigit }
                    digits = newDigits
                    // Zusammensetzen des neuen Frequenz-Strings und Umwandlung in Float
                    val newFreq = newDigits.joinToString(separator = "").toInt().toFloat()
                    onFrequencyChange(newFreq)
                },
                modifier = Modifier.width(50.dp)
            )
        }
    }
}


/*


@Composable
fun DataScreen(navController: NavController, receivedData: String) {

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF11144F), Color(0xFF1F4596)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1500f, 1500f)
    )

    // Überprüfe, ob die Daten gültig sind, bevor sie deserialisiert werden
    if (receivedData.isNotEmpty()) {
        try {
            MainActivity.FunkyRepository.funkyInfo = Json.decodeFromString<FunkyInfo>(receivedData)
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
            .background(gradientBrush), // Grüner Hintergrund für erfolgreichen Datenaustausch
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

                // Erstes Rechteck
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
                                text = MainActivity.FunkyRepository.funkyInfo.frequency.toString(),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Zweites Rechteck
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


 */

//14,000 000 MHz - 14,350 000 MHz in 100 Hz schritten
