package com.beatcontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Background = Color(0xFF080B18)
private val Panel = Color(0xFF12182B)
private val NeonBlue = Color(0xFF36D9FF)
private val NeonPink = Color(0xFFFF3D81)
private val NeonGreen = Color(0xFF7CFFB2)
private val TextWhite = Color(0xFFF4F7FF)
private val Muted = Color(0xFF8C96B5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NeonBlue,
                    background = Background,
                    surface = Panel
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    BeatControlApp()
                }
            }
        }
    }
}

private enum class Screen {
    HOME, GAME, RESULTS, SETTINGS
}

private data class Obstacle(
    val id: Int,
    val lane: Int,
    val y: Float
)

@Composable
private fun BeatControlApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var finalScore by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var soundEnabled by remember { mutableStateOf(true) }
    var sensitivity by remember { mutableIntStateOf(5) }

    when (screen) {
        Screen.HOME -> HomeScreen(
            bestScore = bestScore,
            onPlay = { screen = Screen.GAME },
            onSettings = { screen = Screen.SETTINGS }
        )

        Screen.GAME -> GameScreen(
            sensitivity = sensitivity,
            onGameOver = { score ->
                finalScore = score
                if (score > bestScore) bestScore = score
                screen = Screen.RESULTS
            },
            onExit = { screen = Screen.HOME }
        )

        Screen.RESULTS -> ResultsScreen(
            score = finalScore,
            bestScore = bestScore,
            onRetry = { screen = Screen.GAME },
            onHome = { screen = Screen.HOME }
        )

        Screen.SETTINGS -> SettingsScreen(
            soundEnabled = soundEnabled,
            sensitivity = sensitivity,
            onSoundChange = { soundEnabled = it },
            onSensitivityChange = { sensitivity = it },
            onBack = { screen = Screen.HOME }
        )
    }
}

@Composable
private fun HomeScreen(
    bestScore: Int,
    onPlay: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BEAT",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = NeonBlue
        )

        Text(
            text = "CONTROL",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NeonPink
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "CONTROL THE RHYTHM",
            color = Muted,
            fontSize = 13.sp,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(40.dp))

        NeonButton(
            text = "▶  ИГРАТЬ",
            onClick = onPlay
        )

        Spacer(Modifier.height(14.dp))

        NeonButton(
            text = "⚙  НАСТРОЙКИ",
            onClick = onSettings,
            color = Panel
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = "РЕКОРД: $bestScore",
            color = NeonGreen,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Выбирай дорожку и уклоняйся от препятствий.",
            color = Muted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GameScreen(
    sensitivity: Int,
    onGameOver: (Int) -> Unit,
    onExit: () -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var nextId by remember { mutableIntStateOf(0) }
    var tick by remember { mutableLongStateOf(0L) }

    val obstacles = remember { mutableStateListOf<Obstacle>() }

    val spawnInterval = (850L - score.toLong() * 3L)
        .coerceAtLeast(350L)

    val speed = (0.018f + score * 0.00008f)
        .coerceAtMost(0.045f)

    LaunchedEffect(gameOver, tick) {
        if (gameOver) return@LaunchedEffect

        delay(16L)

        val updated = obstacles.mapNotNull { obstacle ->
            val newY = obstacle.y + speed

            if (newY > 1.12f) {
                score += 10
                combo += 1
                null
            } else {
                obstacle.copy(y = newY)
            }
        }

        obstacles.clear()
        obstacles.addAll(updated)

        val hit = obstacles.any { obstacle ->
            obstacle.lane == playerLane &&
                obstacle.y >= 0.79f &&
                obstacle.y <= 0.96f
        }

        if (hit) {
            gameOver = true
            onGameOver(score)
        } else {
            tick += 1
        }
    }

    LaunchedEffect(gameOver) {
        if (gameOver) return@LaunchedEffect

        while (!gameOver) {
            delay(spawnInterval)

            if (!gameOver) {
                nextId += 1

                val lane = Random.nextInt(0, 3)

                obstacles.add(
                    Obstacle(
                        id = nextId,
                        lane = lane,
                        y = -0.08f
                    )
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✕",
                color = TextWhite,
                fontSize = 26.sp,
                modifier = Modifier
                    .clickable(onClick = onExit)
                    .padding(8.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BEAT CONTROL",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "COMBO  x$combo",
                    color = NeonPink,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("СЧЁТ", color = Muted, fontSize = 11.sp)
                Text(
                    text = score.toString(),
                    color = NeonGreen,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = Color(0xFF0B1020),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gameOver) {
                        detectTapGestures { offset ->
                            if (!gameOver) {
                                val laneWidth = size.width / 3f

                                playerLane = (offset.x / laneWidth)
                                    .toInt()
                                    .coerceIn(0, 2)
                            }
                        }
                    }
            ) {
                val laneWidth = size.width / 3f
                val height = size.height

                // Разделители трёх дорожек.
                for (i in 1..2) {
                    val x = laneWidth * i

                    drawLine(
                        color = NeonBlue.copy(alpha = 0.28f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Линия опасной зоны игрока.
                drawLine(
                    color = NeonPink.copy(alpha = 0.5f),
                    start = Offset(0f, height * 0.88f),
                    end = Offset(size.width, height * 0.88f),
                    strokeWidth = 1.dp.toPx()
                )

                // Игрок.
                val playerX = laneWidth * (playerLane + 0.5f)
                val playerY = height * 0.86f

                drawCircle(
                    color = NeonBlue.copy(alpha = 0.18f),
                    radius = 31.dp.toPx(),
                    center = Offset(playerX, playerY)
                )

                drawCircle(
                    color = NeonBlue,
                    radius = 17.dp.toPx(),
                    center = Offset(playerX, playerY)
                )

                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(playerX, playerY)
                )

                // Падающие препятствия.
                obstacles.forEach { obstacle ->
                    val centerX = laneWidth * (obstacle.lane + 0.5f)
                    val centerY = height * obstacle.y

                    drawRoundRect(
                        color = NeonPink,
                        topLeft = Offset(
                            centerX - laneWidth * 0.23f,
                            centerY - 15.dp.toPx()
                        ),
                        size = Size(
                            laneWidth * 0.46f,
                            30.dp.toPx()
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            8.dp.toPx()
                        )
                    )
                }
            }

            Text(
                text = "НАЖМИ НА ДОРОЖКУ",
                color = Muted.copy(alpha = 0.65f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
            )

            // Три независимые зоны управления внизу.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (lane in 0..2) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable {
                                if (!gameOver) {
                                    playerLane = lane
                                }
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = when (lane) {
                                0 -> "◀"
                                1 -> "●"
                                else -> "▶"
                            },
                            color = if (playerLane == lane) {
                                NeonBlue
                            } else {
                                Muted.copy(alpha = 0.5f)
                            },
                            fontSize = 24.sp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Выбирай одну из трёх дорожек, чтобы избежать столкновения.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultsScreen(
    score: Int,
    bestScore: Int,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            fontSize = 35.sp,
            fontWeight = FontWeight.Black,
            color = NeonPink
        )

        Spacer(Modifier.height(28.dp))

        Text("ТВОЙ СЧЁТ", color = Muted, fontSize = 14.sp)

        Text(
            text = score.toString(),
            color = NeonBlue,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "РЕКОРД: $bestScore",
            color = NeonGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(36.dp))

        NeonButton(
            text = "↻  ЕЩЁ РАЗ",
            onClick = onRetry
        )

        Spacer(Modifier.height(14.dp))

        NeonButton(
            text = "⌂  ГЛАВНОЕ МЕНЮ",
            onClick = onHome,
            color = Panel
        )
    }
}

@Composable
private fun SettingsScreen(
    soundEnabled: Boolean,
    sensitivity: Int,
    onSoundChange: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "НАСТРОЙКИ",
            color = NeonBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(35.dp))

        Text(
            text = "Звук: ${if (soundEnabled) "ВКЛ" else "ВЫКЛ"}",
            color = TextWhite,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(12.dp))

        NeonButton(
            text = if (soundEnabled) "Выключить звук" else "Включить звук",
            onClick = { onSoundChange(!soundEnabled) },
            color = Panel
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Чувствительность: $sensitivity",
            color = TextWhite,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (value in 1..10) {
                Text(
                    text = value.toString(),
                    color = if (value == sensitivity) NeonBlue else Muted,
                    fontWeight = if (value == sensitivity) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    modifier = Modifier
                        .clickable { onSensitivityChange(value) }
                        .padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(35.dp))

        NeonButton(
            text = "НАЗАД",
            onClick = onBack
        )
    }
}

@Composable
private fun NeonButton(
    text: String,
    onClick: () -> Unit,
    color: Color = NeonBlue
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = if (color == NeonBlue) {
                Background
            } else {
                TextWhite
            }
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
