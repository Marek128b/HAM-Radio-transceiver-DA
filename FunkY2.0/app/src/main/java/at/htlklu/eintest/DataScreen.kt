package at.htlklu.eintest.ui

import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import at.htlklu.eintest.MainActivity
import at.htlklu.eintest.data.FunkyInfo
import java.util.Locale
import kotlinx.serialization.json.Json

@Composable
fun DataScreen(navController: NavController) {

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF11144F), Color(0xFF1F4596)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1500f, 1500f)
    )

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
            val tempBoxSize = (maxHeight - (squareSize * 2) - 65.dp - spacing * 3)

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
                            NameCallBox(squareSize)
                        }
                    }
                    // Box für Voltage 10V - 12.6V
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
                            BatteryIndicator(modifier = Modifier.padding(16.dp))

                        }
                    }
                }

                //Frequenz
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
                        .height(tempBoxSize)
                        .clip(RoundedCornerShape(25.dp))
                        .background(Color(0x11FFFFFF))
                        .padding(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x050000000))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TemperatureTracker(Modifier)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FrequencyCheck() {
    // Verwende rememberUpdatedState, um den aktuellen Wert von funkyInfo zu überwachen
    val frequency = rememberUpdatedState(MainActivity.FunkyRepository.funkyInfo.frequency)

    LaunchedEffect(frequency.value) {
        // Überprüfe und passe die Frequenz an, falls sie außerhalb des Bereichs liegt
        if (frequency.value > 14.35f) {
            MainActivity.FunkyRepository.funkyInfo = MainActivity.FunkyRepository.funkyInfo.copy(frequency = 14.35f)
        } else if (frequency.value < 14.0f) {
            MainActivity.FunkyRepository.funkyInfo = MainActivity.FunkyRepository.funkyInfo.copy(frequency = 14.0f)
        }
    }
}

@Composable
fun FrequencyLock(
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val digitWidth = maxWidth / 11  // Dynamische Breite für die einzelnen Digits

        FrequencyCheck()

        // `currentFrequency` direkt mit FunkyRepository synchronisieren
        val currentFrequency by rememberUpdatedState(MainActivity.FunkyRepository.funkyInfo.frequency)

        val formattedFrequency =
            String.format(Locale.US, "%.4f", currentFrequency) // Immer 4 Nachkommastellen
        val parts = formattedFrequency.split(".")

        // Stelle sicher, dass die Nachkommastellen genau 4 Zeichen lang sind
        val decimalPart = parts.getOrNull(1)?.padEnd(4, '0') ?: "0000"

        val integerPart = parts[0].map { it.toString().toInt() }
        var decimalDigits by remember {
            mutableStateOf(decimalPart.map {
                it.toString().toInt()
            })
        }

        LaunchedEffect(currentFrequency) {
            // Aktualisiere decimalDigits, falls sich FunkyRepository ändert
            decimalDigits = decimalPart.map { it.toString().toInt() }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            integerPart.forEach { digit ->
                Box(
                    modifier = Modifier.width(digitWidth),
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

            Box(
                modifier = Modifier.width(digitWidth / 2),
                contentAlignment = Alignment.Center
            ) {
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
                    Box(
                        modifier = Modifier.width(digitWidth / 2),
                        contentAlignment = Alignment.Center
                    ) {
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
                        val newDigits =
                            decimalDigits.toMutableList().apply { this[index] = newDigit }
                        decimalDigits = newDigits

                        val newFrequencyString =
                            "${integerPart.joinToString("")}.${newDigits.joinToString("")}"
                        val newFrequency = newFrequencyString.toFloat()

                        // **Direkt speichern in FunkyRepository**
                        MainActivity.FunkyRepository.funkyInfo =
                            MainActivity.FunkyRepository.funkyInfo.copy(frequency = newFrequency)
                    },
                    modifier = Modifier.width(digitWidth) // Dynamische Breite für die Zahlenräder
                )
            }

            Box(
                modifier = Modifier.width(digitWidth * 3),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MHz",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
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
        displayedValue = value
    }

    Column(
        modifier = modifier.width(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = {
            val newValue = if (displayedValue == 9) 0 else displayedValue + 1
            displayedValue = newValue
            onValueChange(newValue)
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

@Composable
fun NameCallBox(squareSize: Dp) {
    var name by remember { mutableStateOf(MainActivity.FunkyRepository.funkyInfo.name) }
    var call by remember { mutableStateOf(MainActivity.FunkyRepository.funkyInfo.call) }

    var isEditingName by remember { mutableStateOf(false) }
    var isEditingCall by remember { mutableStateOf(false) }

    // **Automatische Aktualisierung bei Änderung von FunkyRepository**
    LaunchedEffect(MainActivity.FunkyRepository.funkyInfo.name) {
        name = MainActivity.FunkyRepository.funkyInfo.name
    }
    LaunchedEffect(MainActivity.FunkyRepository.funkyInfo.call) {
        call = MainActivity.FunkyRepository.funkyInfo.call
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Name (editierbar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .clickable { isEditingName = true }
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isEditingName) {
                TextField(
                    value = name,
                    onValueChange = { newValue ->
                        name = newValue
                        MainActivity.FunkyRepository.funkyInfo =
                            MainActivity.FunkyRepository.funkyInfo.copy(name = newValue) // **Speichern**
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { isEditingName = false })
                )
            } else {
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Trennlinie (dünne weiße Box)
        Box(
            modifier = Modifier
                .width(squareSize - 40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(20.dp)
        )

        // Call (editierbar, etwas kleiner)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .clickable { isEditingCall = true }
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isEditingCall) {
                TextField(
                    value = call,
                    onValueChange = { newValue ->
                        call = newValue
                        MainActivity.FunkyRepository.funkyInfo =
                            MainActivity.FunkyRepository.funkyInfo.copy(call = newValue) // **Speichern**
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { isEditingCall = false })
                )
            } else {
                Text(
                    text = call,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun BatteryIndicator(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Verwende rememberUpdatedState, um sicherzustellen, dass wir immer den neuesten Wert von voltage verwenden
        val voltage by rememberUpdatedState(MainActivity.FunkyRepository.funkyInfo.voltage)

        // Berechne den batteryLevel, der sich immer dynamisch aktualisiert
        var batteryLevel by remember { mutableStateOf((voltage - 10f) / 2.6f) }

        // LaunchedEffect, um den Wert bei Änderungen von FunkyRepository zu aktualisieren
        LaunchedEffect(voltage) {
            batteryLevel =
                (voltage - 10f) / 2.6f // Dies ist eine einfache Berechnung, die sich je nach Bedarf ändern kann
        }

        val roundedBatteryLevel = ((batteryLevel.coerceIn(0f, 1f).toDouble()) * 100).let {
            kotlin.math.ceil(it).toInt() // Rundet auf die nächste ganze Zahl auf
        }

        val batteryLevelString = roundedBatteryLevel.toString() // Umwandeln in String


        BatteryVisual(Modifier, batteryLevel)
        Text(
            text = batteryLevelString + "%",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = MainActivity.FunkyRepository.funkyInfo.voltage.toString() + "V",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }

}

@Composable
fun BatteryVisual(modifier: Modifier = Modifier, batteryLevel: Float) {

    val batteryWidth = 100.dp  // Gesamtbreite der Batterie
    val batteryHeight = 50.dp  // Höhe der Batterie
    val batteryCapWidth = 20.dp // Breite der kleinen Box am Rand
    val batteryPadding = 4.dp   // Innenabstand für das grüne Fülllevel
    val batteryRounded = 10.dp



    Box(
        modifier = modifier
            .width(batteryWidth + batteryCapWidth) // Gesamte Breite inklusive Batterie-Kopf
            .height(batteryHeight)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Kleine Box als Batterie-Kopf (rechts)
        Box(
            modifier = Modifier
                .offset(x = batteryWidth / 2) // Rechts neben die Batterie verschieben
                .width(batteryCapWidth)
                .height(batteryHeight / 2)
                .background(Color.White, RoundedCornerShape(batteryRounded / 2))
        )

        // Äußere Batterie-Box
        Box(
            modifier = Modifier
                .width(batteryWidth + 4.dp)
                .height(batteryHeight + 4.dp)
                .clip(RoundedCornerShape(batteryRounded))
                .background(Color.White)
        ) {
            // Innere graue Box für den Füllstand
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(batteryPadding) // Abstand zur Außenkante
                    .clip(RoundedCornerShape(batteryRounded - batteryPadding))
                    .background(Color.Gray)
            ) {
                // Grüne Box für den aktuellen Füllstand
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(
                            (batteryWidth - batteryPadding * 2) * batteryLevel.coerceIn(0f, 1f)
                        ) // Dynamische Breite des grünen Bereichs basierend auf batteryLevel
                        .background(
                            Color(0xFF199A40),
                            RoundedCornerShape(batteryRounded - batteryPadding)
                        )
                )
            }
        }
    }
}

@Composable
fun TemperatureTracker(modifier: Modifier) {
    // Liste, um die letzten 10 Temperaturwerte zu speichern
    val temperatureHistory = remember { mutableStateOf(MutableList(10) { 0f }) } // Start mit 10 Werten, alle 0

    // Neuen Temperaturwert hinzufügen und die Liste aktualisieren
    LaunchedEffect(MainActivity.FunkyRepository.funkyInfo.temperature) {
        val newTemperature = MainActivity.FunkyRepository.funkyInfo.temperature

        // Wenn es das erste Mal ist, setzen wir den ersten Wert auf den initialen Wert von funkyRepository
        if (temperatureHistory.value.all { it == 0f }) { // Überprüfen, ob alle Werte auf 0f gesetzt sind
            // Setze alle 10 Werte auf den ersten empfangenen Wert
            temperatureHistory.value = MutableList(10) { newTemperature }
        } else {
            // Füge den neuen Wert hinzu und entferne den ältesten
            val updatedHistory = temperatureHistory.value.toMutableList()
            updatedHistory.removeAt(0) // Entferne den ältesten Wert
            updatedHistory.add(newTemperature) // Füge den neuen Wert hinzu
            temperatureHistory.value = updatedHistory
        }
    }

    // Holen der letzten 10 Temperaturwerte
    val temperatures = temperatureHistory.value

    // Zeige das Diagramm mit den letzten 10 Temperaturwerten
    LineChartWithAxes(modifier, temperatures)
}

@Composable
fun LineChartWithAxes(modifier: Modifier, data: List<Float>) {
    // X-Achse: Indizes von 0 bis 9 (letzte 10 Werte)
    val xValues = data.indices.toList()

    // Y-Achse: Temperaturwerte
    val yValues = data

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(30.dp)
    ) {
        val canvasWidth = size.width.minus(32)
        val canvasHeight = size.height

        // Abstand der X-Achse
        val xStep = canvasWidth / (xValues.size - 1)

        // Zeichne die Linien
        for (i in 1 until xValues.size) {
            val startX = xStep * (i - 1)
            val startY = canvasHeight * (1 - yValues[i - 1] / 100)
            val endX = xStep * i
            val endY = canvasHeight * (1 - yValues[i] / 100)

            drawLine(
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                color = Color.Green,
                strokeWidth = 5f
            )
        }

        // Zeichne X-Achse (Markierungen)
        for (i in xValues.indices) {
            val x = xStep * i
            drawLine(
                start = Offset(x, canvasHeight),
                end = Offset(x, canvasHeight + 10.dp.toPx()),
                color = Color.White,
                strokeWidth = 4f
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "$i", // X-Wert
                    x - 10.dp.toPx(),
                    canvasHeight + 25.dp.toPx(),
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                    }
                )
            }
        }

        // Zeichne Y-Achse (Markierungen)
        val yStep = canvasHeight / 5 // 5 Y-Achsenmarkierungen
        for (i in 0..5) {
            val y = canvasHeight - (yStep * i)
            drawLine(
                start = Offset(0f, y),
                end = Offset(-10.dp.toPx(), y),
                color = Color.White,
                strokeWidth = 4f
            )

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${(100f / 5 * i).toInt()}",
                    -30.dp.toPx(),
                    y + 10.dp.toPx(),
                    Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 30f
                    }
                )
            }
        }

        // Aktuellen Temperaturwert anzeigen
        val currentTemperature = yValues.last()
        val lastX = xStep * (xValues.size - 1)
        val lastY = canvasHeight * (1 - currentTemperature / 100)

        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "${currentTemperature.toInt()}°C",
                lastX + 10.dp.toPx(),
                lastY - 10.dp.toPx(),
                Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 40f
                    typeface = Typeface.DEFAULT_BOLD
                }
            )
        }
    }
}
