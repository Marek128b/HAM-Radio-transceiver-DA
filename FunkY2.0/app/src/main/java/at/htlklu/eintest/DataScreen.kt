package at.htlklu.eintest.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import at.htlklu.eintest.MainActivity
import at.htlklu.eintest.data.FunkyInfo
import kotlinx.serialization.json.Json

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
            Log.d("DataScreen", "Erfolgreich deserialisiert: ${MainActivity.FunkyRepository.funkyInfo}")
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Empfangene Daten:",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Wenn die Deserialisierung erfolgreich war, zeige die Werte an
            if (MainActivity.FunkyRepository.funkyInfo != null) {
                // Anzeigen der deserialisierten Werte
                Text(
                    text = "Name: ${MainActivity.FunkyRepository.funkyInfo.name}\n" +
                            "Frequenz: ${MainActivity.FunkyRepository.funkyInfo.frequency}\n" +
                            "Volt: ${MainActivity.FunkyRepository.funkyInfo.voltage}\n" +
                            "Temperatur: ${MainActivity.FunkyRepository.funkyInfo.temperature}\n" ,
                    fontSize = 18.sp,
                    color = Color.White
                )
            } else {
                // Falls die Deserialisierung fehlschlägt, zeige den rohen empfangenen JSON-String an
                Text(
                    text = "Fehlerhafte Daten: $receivedData",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                navController.popBackStack() // Zurück zur vorherigen Seite
            }) {
                Text("Zurück")
            }
        }
    }
}
