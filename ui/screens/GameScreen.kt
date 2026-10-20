package com.beatcontrol.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beatcontrol.app.ui.theme.DarkBackground
import com.beatcontrol.app.ui.theme.NeonCyan
import com.beatcontrol.app.ui.theme.NeonPink
import com.beatcontrol.app.ui.theme.SurfaceDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Obstacle(
    var y: Float,
    val lane: Int,
    val speed: Float
)

@Composable
fun GameScreen(
    levelId: String,
    onGameOver: (score: Int, combo: Int, accuracy: Float) -> Unit,
    onPauseClick: () -> Unit
) {
    var score by remember { mutableStateOf(0) }
    var combo by remember { mutableStateOf(0) }
    var maxCombo by remember { mutableStateOf(0) }
    var misses by remember { mutableStateOf(0) }
    var hits by remember { mutableStateOf(0) }
    
    // Игровая дорожка игрока: 0 (левая), 1 (центральная), 2 (правая)
    var playerLane by remember { mutableStateOf(1) }
    
    // Список падающих препятствий
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    var isPlaying by remember { mutableStateOf(true) }

    // Игровой цикл и спавнер
    LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        
        // Поток генерации препятствий
        val spawnJob = launch {
            while (isPlaying) {
                delay(1000L) // Интервал появления
                val randomLane = Random.nextInt(0, 3)
                obstacles.add(Obstacle(y = 0f, lane = randomLane, speed = 700f))
            }
        }

        // Основной игровой цикл (60 FPS)
        var lastTime = System.nanoTime()
        while (isPlaying) {
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastTime) / 1_000_000_000f
            lastTime = currentTime

            val iterator = obstacles.iterator()
            while (iterator.hasNext()) {
                val obs = iterator.next()
                obs.y += obs.speed * deltaTime

                // Проверка достижения нижней границы экрана
                if (obs.y > 1600f) {
                    iterator.remove()
                    if (obs.lane == playerLane) {
                        // Успешное уклонение / сбор
                        score += 100 * (1 + combo / 10)
                        combo++
                        hits++
                        if (combo > maxCombo) maxCombo = combo
                    } else {
                        // Промах / столкновение
                        combo = 0
                        misses++
                        if (misses >= 5) {
                            isPlaying = false
                            val total = hits + misses
                            val accuracy = if (total > 0) hits.toFloat() / total * 100f else 0f
                            onGameOver(score, maxCombo, accuracy)
                        }
                    }
                }
            }

            withFrameNanos { }
        }
        spawnJob.cancel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Управление: тап по левой или правой стороне экрана
                        if (offset.x < size.width / 2) {
                            if (playerLane > 0) playerLane--
                        } else {
                            if (playerLane < 2) playerLane++
                        }
                    }
                )
            }
    ) {
        // Отрисовка игровых элементов через Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val laneWidth = size.width / 3f

            // Разделители дорожек
            for (i in 1..2) {
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(laneWidth * i, 0f),
                    end = Offset(laneWidth * i, size.height),
                    strokeWidth = 3f
                )
            }

            // Отрисовка препятствий (неоново-розовые круги)
            for (obs in obstacles) {
                val xCenter = laneWidth * obs.lane + laneWidth / 2f
                drawCircle(
                    color = NeonPink,
                    radius = 45f,
                    center = Offset(xCenter, obs.y)
                )
            }

            // Отрисовка игрока (неоново-циановый объект)
            val playerX = laneWidth * playerLane + laneWidth / 2f
            val playerY = size.height - 250f
            drawCircle(
                color = NeonCyan,
                radius = 55f,
                center = Offset(playerX, playerY)
            )
        }

        // Интерфейс поверх игры (Счёт, комбо, пауза)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "СЧЁТ: $score", color = NeonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(text = "КОМБО: x$combo", color = NeonPink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { 
                    isPlaying = false
                    onPauseClick() 
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
            ) {
                Text(text = "ПАУЗА", color = Color.White)
            }
        }
    }
}
