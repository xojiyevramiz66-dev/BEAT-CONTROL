package com.beatcontrol.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultsScreen(
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    val darkBackground = Color(0xFF0A0A12)
    val neonCyan = Color(0xFF00F0FF)
    val neonPink = Color(0xFFFF007F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
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
                color = neonCyan
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ИГРА ЗАВЕРШЕНА",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = neonPink
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Счёт: 1000", fontSize = 22.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Макс. комбо: x10", fontSize = 18.sp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Точность: 95.5%", fontSize = 18.sp, color = Color.LightGray)

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
            ) {
                Text(text = "ПОВТОРИТЬ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = neonPink)
            ) {
                Text(text = "В МЕНЮ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
