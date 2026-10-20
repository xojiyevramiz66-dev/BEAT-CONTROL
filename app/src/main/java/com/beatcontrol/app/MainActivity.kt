
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlin.math.min
import kotlin.random.Random

private val Background = Color(0xFF080B18)
private val Panel = Color(0xFF12182B)
private val NeonBlue = Color(0xFF36D9FF)
private val NeonPink = Color(0xFFFF3D81)
private val NeonGreen = Color(0xFF7CFFB2)
private val TextWhite = Color(0xFFF4F7FF)
private val Muted = Color(0xFF8C96B5)
private val Gold = Color(0xFFFFD166)

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

    var bestScore by remember {
        mutableIntStateOf(preferences.getInt("best_score", 0))
    }

    var soundEnabled by remember {
        mutableStateOf(
            preferences.getBoolean("sound_enabled", true)
        )
    }

    var sensitivity by remember {
        mutableIntStateOf(
            preferences.getInt("sensitivity", 5).coerceIn(1, 10)
        )
    }

    fun saveScore(score: Int) {
        if (score > bestScore) {
            bestScore = score
            preferences.edit()
                .putInt("best_score", score)
                .apply()
        }
    }

    fun changeSound(enabled: Boolean) {
        soundEnabled = enabled
        preferences.edit()
            .putBoolean("sound_enabled", enabled)
            .apply()
    }

    fun changeSensitivity(value: Int) {
        sensitivity = value.coerceIn(1, 10)
        preferences.edit()
            .putInt("sensitivity", sensitivity)
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
                saveScore(score)
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
            onSoundChange = ::changeSound,
            onSensitivityChange = ::changeSensitivity,
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
            fontSize = 52.sp,
            fontWeight = FontWeight.Black,
            color = NeonBlue
        )

        Text(
            text = "CONTROL",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NeonPink
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "FEEL THE RHYTHM",
            color = Muted,
            fontSize = 13.sp,
            letterSpacing = 3.sp
        )

        Spacer(Modifier.height(38.dp))

        NeonButton("▶  ИГРАТЬ", onPlay)

        Spacer(Modifier.height(14.dp))

        NeonButton(
            "⚙  НАСТРОЙКИ",
            onSettings,
            color = Panel
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text = "РЕКОРД: $bestScore",
            color = NeonGreen,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = "Уклоняйся от препятствий и нажимай в ритм!",
            color = Muted,
            textAlign = TextAlign.Center,
            fontSize = 13.sp
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

    var feedback by remember { mutableStateOf("ГОТОВ?") }
    var feedbackColor by remember { mutableStateOf(NeonBlue) }

    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastFrameTime by remember {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    val gameStartedAt = remember {
        mutableLongStateOf(SystemClock.elapsedRealtime())
    }

    val pausedDuration = remember { mutableLongStateOf(0L) }
    val pauseStartedAt = remember { mutableLongStateOf(0L) }

    val obstacles = remember {
        mutableStateListOf<Obstacle>()
    }

    val latestScore by rememberUpdatedState(score)
    val latestPlayerLane by rememberUpdatedState(playerLane)
    val latestGameOver by rememberUpdatedState(gameOver)
    val latestPaused by rememberUpdatedState(isPaused)
    val latestOnGameOver by rememberUpdatedState(onGameOver)

    val bpm = (120 + score / 50).coerceAtMost(180)
    val beatDuration = 60_000L / bpm

    val toneGenerator = remember(soundEnabled) {
        if (soundEnabled) {
            runCatching {
                ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            }.getOrNull()
        } else {
            null
        }
    }

    // Эффекты всегда используют актуальный генератор звука.
    val latestToneGenerator by rememberUpdatedState(toneGenerator)

    DisposableEffect(toneGenerator) {
        onDispose {
            runCatching {
                toneGenerator?.release()
            }
        }
    }

    fun playTone(tone: Int, duration: Int = 70) {
        runCatching {
            latestToneGenerator?.startTone(tone, duration)
        }
    }

    fun togglePause() {
        if (gameOver) return

        val now = SystemClock.elapsedRealtime()

        if (isPaused) {
            pausedDuration.longValue +=
                (now - pauseStartedAt.longValue).coerceAtLeast(0L)

            lastFrameTime = now
            isPaused = false
        } else {
            pauseStartedAt.longValue = now
            isPaused = true
        }
    }

    fun moveToLane(target: Int) {
        if (gameOver || isPaused) return

        val now = SystemClock.elapsedRealtime()

        if (now - lastTapTime < 120L) return
        lastTapTime = now

        val destination = target.coerceIn(0, 2)

        playerLane = if (sensitivity <= 3) {
            when {
                destination > playerLane ->
                    (playerLane + 1).coerceAtMost(2)

                destination < playerLane ->
                    (playerLane - 1).coerceAtLeast(0)

                else -> playerLane
            }
        } else {
            destination
        }

        val elapsed = (
            now - gameStartedAt.longValue - pausedDuration.longValue
        ).coerceAtLeast(0L)

        val phase = elapsed % beatDuration
        val distanceToBeat = min(phase, beatDuration - phase)

        when {
            distanceToBeat <= 85L -> {
                feedback = "PERFECT!"
                feedbackColor = Gold
                score += 25 + (combo / 10) * 5
                combo++
                playTone(ToneGenerator.TONE_PROP_ACK)
            }

            distanceToBeat <= 170L -> {
                feedback = "GOOD!"
                feedbackColor = NeonGreen
                score += 10 + (combo / 10) * 2
                combo++
                playTone(ToneGenerator.TONE_PROP_BEEP)
            }

            else -> {
                feedback = "KEEP THE BEAT"
                feedbackColor = Muted
                combo = 0
                playTone(ToneGenerator.TONE_PROP_BEEP2, 45)
            }
        }
    }

    // Обновление движения и столкновений.
    LaunchedEffect(gameOver, isPaused) {
        if (gameOver || isPaused) return@LaunchedEffect

        lastFrameTime = SystemClock.elapsedRealtime()

        while (!gameOver && !isPaused) {
            delay(16L)

            val now = SystemClock.elapsedRealtime()
            val delta = (
                now - lastFrameTime
            ).coerceIn(1L, 50L) / 1000f

            lastFrameTime = now

            val speed = (
                0.95f + latestScore * 0.0015f
            ).coerceAtMost(1.8f)

            var passedCount = 0
            var hit = false

            val updated = obstacles.mapNotNull { obstacle ->
                val newY = obstacle.y + speed * delta

                // Учитываем прохождение препятствия через зону игрока.
                val crossesPlayerZone =
                    obstacle.y <= 0.91f && newY >= 0.80f

                if (
                    obstacle.lane == latestPlayerLane &&
                    crossesPlayerZone
                ) {
                    hit = true
                }

                if (newY > 1.08f) {
                    passedCount++
                    null
                } else {
                    obstacle.copy(y = newY)
                }
            }

            obstacles.clear()
            obstacles.addAll(updated)

            if (passedCount > 0) {
                score += passedCount * 10
            }

            if (hit && !latestGameOver) {
                gameOver = true
                playTone(ToneGenerator.TONE_PROP_NACK, 220)

                // Берём актуальный счёт, а не значение из старого кадра.
                latestOnGameOver(latestScore + passedCount * 10)
                break
            }
        }
    }

    // Создание препятствий.
    LaunchedEffect(gameOver, isPaused) {
        if (gameOver || isPaused) return@LaunchedEffect

        while (!gameOver && !isPaused) {
            val currentBpm =
                (120 + latestScore / 50).coerceAtMost(180)

            val spawnInterval = (60_000L / currentBpm) * 2L
            delay(spawnInterval)

            if (!gameOver && !isPaused) {
                nextId++

                val occupiedLanes = obstacles
                    .filter { it.y < 0.12f }
                    .map { it.lane }
                    .toSet()

                val available = (0..2).filterNot {
                    it in occupiedLanes
                }

                val lane = if (available.isNotEmpty()) {
                    available.random()
                } else {
                    Random.nextInt(0, 3)
                }

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

    // Метроном.
    LaunchedEffect(gameOver, isPaused, bpm, soundEnabled) {
        if (gameOver || isPaused || !soundEnabled) {
            return@LaunchedEffect
        }

        while (!gameOver && !isPaused) {
            val now = SystemClock.elapsedRealtime()

            val elapsed = (
                now - gameStartedAt.longValue - pausedDuration.longValue
            ).coerceAtLeast(0L)

            val phase = elapsed % beatDuration

            val wait = if (phase == 0L) {
                beatDuration
            } else {
                beatDuration - phase
            }

            delay(wait.coerceAtLeast(1L))

            if (!latestGameOver && !latestPaused) {
                playTone(ToneGenerator.TONE_PROP_BEEP, 35)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✕",
                color = TextWhite,
                fontSize = 25.sp,
                modifier = Modifier
                    .clickable(onClick = onExit)
                    .padding(8.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BEAT CONTROL",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Text(
                    text = "$bpm BPM  •  COMBO x$combo",
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

        Spacer(Modifier.height(8.dp))

        NeonButton(
            text = if (isPaused) "▶ ПРОДОЛЖИТЬ" else "Ⅱ ПАУЗА",
            onClick = { togglePause() },
            color = Panel
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = feedback,
            color = feedbackColor,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    Color(0xFF0B1020),
                    RoundedCornerShape(18.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(gameOver, isPaused, sensitivity, bpm) {
                        detectTapGestures { offset ->
                            val laneWidth = size.width / 3f
                            val lane = (offset.x / laneWidth)
                                .toInt()
                                .coerceIn(0, 2)

                            moveToLane(lane)
                        }
                    }
            ) {
                val laneWidth = size.width / 3f
                val h = size.height

                for (i in 1..2) {
                    val x = laneWidth * i

                    drawLine(
                        color = NeonBlue.copy(alpha = 0.30f),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                drawLine(
                    color = NeonPink.copy(alpha = 0.65f),
                    start = Offset(0f, h * 0.87f),
                    end = Offset(size.width, h * 0.87f),
                    strokeWidth = 2.dp.toPx()
                )

                val playerX = laneWidth * (playerLane + 0.5f)
                val playerY = h * 0.86f

                drawCircle(
                    color = NeonBlue.copy(alpha = 0.18f),
                    radius = 34.dp.toPx(),
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
                    val x = laneWidth * (obstacle.lane + 0.5f)
                    val y = h * obstacle.y

                    drawRoundRect(
                        color = NeonPink,
                        topLeft = Offset(
                            x - laneWidth * 0.22f,
                            y - 15.dp.toPx()
                        ),
                        size = Size(
                            laneWidth * 0.44f,
                            30.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.16f),
                        topLeft = Offset(
                            x - laneWidth * 0.17f,
                            y - 10.dp.toPx()
                        ),
                        size = Size(
                            laneWidth * 0.34f,
                            4.dp.toPx()
                        ),
                        cornerRadius = CornerRadius(3.dp.toPx())
                    )
                }
            }

            Text(
                text = "НАЖИМАЙ НА ДОРОЖКУ",
                color = Muted.copy(alpha = 0.75f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
            )

            if (isPaused && !gameOver) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Panel.copy(alpha = 0.98f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ПАУЗА",
                        color = NeonBlue,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )

                    Spacer(Modifier.height(14.dp))

                    NeonButton(
                        text = "▶ ПРОДОЛЖИТЬ",
                        onClick = { togglePause() }
                    )

                    Spacer(Modifier.height(10.dp))

                    NeonButton(
                        text = "В ГЛАВНОЕ МЕНЮ",
                        onClick = onExit,
                        color = Background
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            for (lane in 0..2) {
                Text(
                    text = when (lane) {
                        0 -> "◀ ЛЕВО"
                        1 -> "● ЦЕНТР"
                        else -> "ПРАВО ▶"
                    },
                    color = if (playerLane == lane) NeonBlue else Muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { moveToLane(lane) }
                        .padding(vertical = 10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
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
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = NeonPink
        )

        Spacer(Modifier.height(28.dp))

        Text("ТВОЙ СЧЁТ", color = Muted, fontSize = 14.sp)

        Text(
            text = score.toString(),
            color = NeonBlue,
            fontSize = 58.sp,
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "РЕКОРД: $bestScore",
            color = NeonGreen,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(36.dp))

        NeonButton("↻  ЕЩЁ РАЗ", onRetry)

        Spacer(Modifier.height(14.dp))

        NeonButton(
            "⌂  ГЛАВНОЕ МЕНЮ",
            onHome,
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
            fontWeight = FontWeight.Black
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Звук: ${if (soundEnabled) "ВКЛ" else "ВЫКЛ"}",
            color = TextWhite,
            fontSize = 18.sp
        )

        Spacer(Modifier.height(12.dp))

        NeonButton(
            text = if (soundEnabled) {
                "ВЫКЛЮЧИТЬ ЗВУК"
            } else {
                "ВКЛЮЧИТЬ ЗВУК"
            },
            onClick = { onSoundChange(!soundEnabled) },
            color = Panel
        )

        Spacer(Modifier.height(28.dp))

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
            text = "Низкая чувствительность ограничивает переход одной дорожкой.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        NeonButton("НАЗАД", onBack)
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
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
