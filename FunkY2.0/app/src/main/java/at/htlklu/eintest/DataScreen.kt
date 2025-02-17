package at.htlklu.eintest.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import at.htlklu.eintest.data.FunkyInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Composable
fun DataScreen(navController: NavController, receivedData: String) {

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF11144F), Color(0xFF1F4596)),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(1500f, 1500f)
    )

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

            //val funkyInfo: FunkyInfo = Json.decodeFromString(receivedData)

            // Anzeigen der empfangenen Daten
            Text(
                text = receivedData,
                fontSize = 18.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                navController.popBackStack() // Zurück zur vorherigen Seite
            }) {
                Text("Zurück")
            }
        }
    }
}
