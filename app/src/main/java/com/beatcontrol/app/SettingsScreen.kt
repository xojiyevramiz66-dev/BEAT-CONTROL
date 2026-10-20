package com.beatcontrol.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var musicVol by remember { mutableStateOf(1.0f) }
    var soundVol by remember { mutableStateOf(1.0f) }
    var sens by remember { mutableStateOf(1.0f) }

    val darkBackground = Color(0xFF0A0A12)
    val neonCyan = Color(0xFF00F0FF)
    val neonPink = Color(0xFFFF007F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "НАСТРОЙКИ",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = neonCyan,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
            )

            // Громкость музыки
            Text(text = "Громкость музыки: ${(musicVol * 100).toInt()}%", color = Color.White)
            Slider(
                value = musicVol,
                onValueChange = { musicVol = it },
                colors = SliderDefaults.colors(thumbColor = neonCyan, activeTrackColor = neonCyan)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Громкость звуков
            Text(text = "Громкость эффектов: ${(soundVol * 100).toInt()}%", color = Color.White)
            Slider(
                value = soundVol,
                onValueChange = { soundVol = it },
                colors = SliderDefaults.colors(thumbColor = neonPink, activeTrackColor = neonPink)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Чувствительность
            Text(text = "Чувствительность: ${(sens * 100).toInt()}%", color = Color.White)
            Slider(
                value = sens,
                onValueChange = { sens = it },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = neonCyan, activeTrackColor = neonCyan)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
            ) {
                Text(text = "НАЗАД", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
