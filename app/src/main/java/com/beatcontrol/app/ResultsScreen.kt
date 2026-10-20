package com.beatcontrol.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun ResultsScreen(
    score: Int,
    maxCombo: Int,
    accuracy: Float,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val isNewRecord = remember { prefs.saveHighScore("normal", score) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "ИТОГИ УРОВНЯ",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )

            if (isNewRecord) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "НОВЫЙ РЕКОРД!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPink
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Счёт: $score", fontSize = 22.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Макс. комбо: x$maxCombo", fontSize = 18.sp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Точность: ${String.format("%.1f", accuracy)}%", fontSize = 18.sp, color = Color.LightGray)

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(text = "ПОВТОРИТЬ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink)
            ) {
                Text(text = "В МЕНЮ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
