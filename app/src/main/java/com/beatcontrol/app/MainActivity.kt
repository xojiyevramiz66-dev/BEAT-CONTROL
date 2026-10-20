package com.beatcontrol.app

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.SystemClock
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    val preferences = remember(context) {
        context.getSharedPreferences(
            "beat_control",
            Context.MODE_PRIVATE
        )
    }

    var screen by remember { mutableStateOf(Screen.HOME) }
    var finalScore by remember { mutableIntStateOf(0) }

    var bestScore by remember(preferences) {
        mutableIntStateOf(
            preferences.getInt("best_score", 0)
        )
    }

    var soundEnabled by remember(preferences) {
        mutableStateOf(
            preferences.getBoolean("sound_enabled", true)
        )
    }

    var sensitivity by remember(preferences) {
        mutableIntStateOf(
            preferences.getInt("sensitivity", 5)
                .coerceIn(1, 10)
        )
    }

    fun saveSensitivity(value: Int) {
        sensitivity = value.coerceIn(1, 10)
        preferences.edit()
            .putInt("sensitivity", sensitivity)
            .apply()
    }

    fun saveSound(value: Boolean) {
        soundEnabled = value
        preferences.edit()
            .putBoolean("sound_enabled", value)
            .apply()
    }

    when (screen) {
        Screen.HOME -> HomeScreen(
            bestScore = bestScore,
            onPlay = { screen = Screen.GAME },
            onSettings = { screen = Screen.SETTINGS }
        )

        Screen.GAME -> GameScreen(
            sensitivity = sensitivity,
            soundEnabled = soundEnabled,
            onGameOver = { score ->
                finalScore = score

                if (score > bestScore) {
                    bestScore = score
                    preferences.edit()
                        .putInt("best_score", score)
                        .apply()
                }

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
            onSoundChange = ::saveSound,
            onSensitivityChange = ::saveSensitivity,
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
    soundEnabled: Boolean,
    onGameOver: (Int) -> Unit,
    onExit: () -> Unit
) {
    var playerLane by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }

    var gameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    var nextId by remember { mutableIntStateOf(0) }
    var lastFrameTime by remember {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    val obstacles = remember {
        mutableStateListOf<Obstacle>()
    }

    val toneGenerator = remember(soundEnabled) {
        if (soundEnabled) {
            runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 75)
            }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(toneGenerator) {
        onDispose {
            toneGenerator?.release()
        }
    }

    // Движение зависит от прошедшего времени, а не от FPS.
    LaunchedEffect(gameOver, isPaused) {
        if (gameOver || isPaused) {
            return@LaunchedEffect
        }

        lastFrameTime = SystemClock.elapsedRealtime()

        while (!gameOver && !isPaused) {
            delay(16L)

            val now = SystemClock.elapsedRealtime()

            val deltaSeconds =
                ((now - lastFrameTime).coerceIn(1L, 50L)) / 1000f

            lastFrameTime = now

            // Скорость постепенно увеличивается с ростом счёта.
            val speedPerSecond =
                (1.08f + score * 0.0048f).coerceAtMost(2.7f)

            val updated = obstacles.mapNotNull { obstacle ->
                val newY = obstacle.y + speedPerSecond * deltaSeconds

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

                runCatching {
                    toneGenerator?.startTone(
                        ToneGenerator.TONE_PROP_NACK,
                        220
                    )
                }

                onGameOver(score)
            }
        }
    }

    // Интервал появления пересчитывается при каждом цикле.
    LaunchedEffect(gameOver, isPaused) {
        if (gameOver || isPaused) {
            return@LaunchedEffect
        }

        while (!gameOver && !isPaused) {
            val spawnInterval =
                (850L - score.toLong() * 3L).coerceAtLeast(350L)

            delay(spawnInterval)

            if (!gameOver && !isPaused) {
                nextId += 1

                obstacles.add(
                    Obstacle(
                        id = nextId,
                        lane = Random.nextInt(0, 3),
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "СЧЁТ",
                    color = Muted,
                    fontSize = 11.sp
                )

                Text(
                    text = score.toString(),
                    color = NeonGreen,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        NeonButton(
            text = if (isPaused) "▶ ПРОДОЛЖИТЬ" else "Ⅱ ПАУЗА",
            onClick = {
                isPaused = !isPaused
                lastFrameTime = SystemClock.elapsedRealtime()
            },
            color = Panel
        )

        Spacer(Modifier.height(10.dp))

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
                    .pointerInput(gameOver, isPaused, sensitivity) {
                        detectTapGestures { offset ->
                            if (!gameOver && !isPaused) {
                                val laneWidth = size.width / 3f

                                val targetLane =
                                    (offset.x / laneWidth)
                                        .toInt()
                                        .coerceIn(0, 2)

                                // Низкая чувствительность ограничивает
                                // переключение одной соседней дорожкой.
                                playerLane = if (sensitivity <= 3) {
                                    when {
                                        targetLane > playerLane ->
                                            (playerLane + 1).coerceAtMost(2)

                                        targetLane < playerLane ->
                                            (playerLane - 1).coerceAtLeast(0)

                                        else -> playerLane
                                    }
                                } else {
                                    targetLane
                                }
                            }
                        }
                    }
            ) {
                val laneWidth = size.width / 3f
                val canvasHeight = size.height

                for (i in 1..2) {
                    val x = laneWidth * i

                    drawLine(
                        color = NeonBlue.copy(alpha = 0.28f),
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                drawLine(
                    color = NeonPink.copy(alpha = 0.5f),
                    start = Offset(0f, canvasHeight * 0.88f),
                    end = Offset(size.width, canvasHeight * 0.88f),
                    strokeWidth = 1.dp.toPx()
                )

                val playerX = laneWidth * (playerLane + 0.5f)
                val playerY = canvasHeight * 0.86f

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

                obstacles.forEach { obstacle ->
                    val centerX =
                        laneWidth * (obstacle.lane + 0.5f)

                    val centerY = canvasHeight * obstacle.y

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
                        cornerRadius = CornerRadius(8.dp.toPx())
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

            // Нижние зоны управления остаются доступными.
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
                                if (!gameOver && !isPaused) {
                                    playerLane = if (sensitivity <= 3) {
                                        when {
                                            lane > playerLane ->
                                                (playerLane + 1).coerceAtMost(2)

                                            lane < playerLane ->
                                                (playerLane - 1).coerceAtLeast(0)

                                            else -> playerLane
                                        }
                                    } else {
                                        lane
                                    }
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

            if (isPaused && !gameOver) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Panel.copy(alpha = 0.97f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ПАУЗА",
                        color = NeonBlue,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.height(12.dp))

                    NeonButton(
                        text = "▶ ПРОДОЛЖИТЬ",
                        onClick = {
                            isPaused = false
                            lastFrameTime = SystemClock.elapsedRealtime()
                        }
                    )
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

        Text(
            text = "ТВОЙ СЧЁТ",
            color = Muted,
            fontSize = 14.sp
        )

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
            text = if (soundEnabled) {
                "Выключить звук"
            } else {
                "Включить звук"
            },
            onClick = { onSoundChange(!soundEnabled) },
            color = Panel
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = "Чувствительность: $sensitivity",
            color = TextWhite,
            fontSize = 18.sp
        )

        Slider(
            value = sensitivity.toFloat(),
            onValueChange = {
                onSensitivityChange(it.toInt().coerceIn(1, 10))
            },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Низкая: переключение по одной дорожке. Высокая: мгновенный переход.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

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
