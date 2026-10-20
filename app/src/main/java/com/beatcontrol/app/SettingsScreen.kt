package com.beatcontrol.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatcontrol.app.data.PreferencesManager
import com.beatcontrol.app.ui.theme.DarkBackground
import com.beatcontrol.app.ui.theme.NeonCyan
import com.beatcontrol.app.ui.theme.NeonPink

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var musicVol by remember { mutableStateOf(prefs.musicVolume) }
    var soundVol by remember { mutableStateOf(prefs.soundVolume) }
    var sens by remember { mutableStateOf(prefs.sensitivity) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
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
                color = NeonCyan,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp)
            )

            // Громкость музыки
            Text(text = "Громкость музыки: ${(musicVol * 100).toInt()}%", color = Color.White)
            Slider(
                value = musicVol,
                onValueChange = {
                    musicVol = it
                    prefs.musicVolume = it
                },
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Громкость звуков
            Text(text = "Громкость эффектов: ${(soundVol * 100).toInt()}%", color = Color.White)
            Slider(
                value = soundVol,
                onValueChange = {
                    soundVol = it
                    prefs.soundVolume = it
                },
                colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Чувствительность
            Text(text = "Чувствительность: ${(sens * 100).toInt()}%", color = Color.White)
            Slider(
                value = sens,
                onValueChange = {
                    sens = it
                    prefs.sensitivity = it
                },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(text = "НАЗАД", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
