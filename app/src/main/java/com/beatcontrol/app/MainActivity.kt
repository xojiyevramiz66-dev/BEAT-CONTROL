package com.beatcontrol.app

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
            MaterialTheme(colorScheme = darkColorScheme(primary = NeonBlue, background = Background, surface = Panel)) {
                Surface(Modifier.fillMaxSize(), color = Background) { BeatControlApp() }
            }
        }
    }
}

private enum class Screen { HOME, GAME, RESULTS, SETTINGS }
private data class Obstacle(val id: Int, val lane: Int, val y: Float)

@Composable
private fun BeatControlApp() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("beat_control", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf(Screen.HOME) }
    var finalScore by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(prefs.getInt("best_score", 0)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var sensitivity by remember { mutableIntStateOf(prefs.getInt("sensitivity", 5).coerceIn(1, 10)) }
    var musicUri by remember { mutableStateOf(prefs.getString("music_uri", null)?.let(Uri::parse)) }
    var bpm by remember { mutableIntStateOf(prefs.getInt("music_bpm", 120).coerceIn(60, 200)) }
    var beatOffsetMs by remember { mutableIntStateOf(prefs.getInt("beat_offset_ms", 0).coerceIn(-1000, 1000)) }

    fun saveBest(score: Int) {
        if (score > bestScore) {
            bestScore = score
            prefs.edit().putInt("best_score", score).apply()
        }
    }

    when (screen) {
        Screen.HOME -> HomeScreen(bestScore, onPlay = { screen = Screen.GAME }, onSettings = { screen = Screen.SETTINGS })
        Screen.GAME -> GameScreen(
            sensitivity = sensitivity,
            soundEnabled = soundEnabled,
            musicUri = musicUri,
            bpm = bpm,
            beatOffsetMs = beatOffsetMs,
            onMusicUriChange = {
                musicUri = it
                prefs.edit().putString("music_uri", it.toString()).apply()
            },
            onBpmChange = {
                bpm = it.coerceIn(60, 200)
                prefs.edit().putInt("music_bpm", bpm).apply()
            },
            onBeatOffsetChange = {
                beatOffsetMs = it.coerceIn(-1000, 1000)
                prefs.edit().putInt("beat_offset_ms", beatOffsetMs).apply()
            },
            onGameOver = { score -> finalScore = score; saveBest(score); screen = Screen.RESULTS },
            onExit = { screen = Screen.HOME }
        )
        Screen.RESULTS -> ResultsScreen(finalScore, bestScore, onRetry = { screen = Screen.GAME }, onHome = { screen = Screen.HOME })
        Screen.SETTINGS -> SettingsScreen(
            soundEnabled, sensitivity,
            onSoundChange = { soundEnabled = it; prefs.edit().putBoolean("sound_enabled", it).apply() },
            onSensitivityChange = { sensitivity = it; prefs.edit().putInt("sensitivity", it).apply() },
            onBack = { screen = Screen.HOME }
        )
    }
}

@Composable
private fun HomeScreen(bestScore: Int, onPlay: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("BEAT", fontSize = 52.sp, fontWeight = FontWeight.Black, color = NeonBlue)
        Text("CONTROL", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = NeonPink)
        Spacer(Modifier.height(10.dp))
        Text("FEEL THE RHYTHM", color = Muted, fontSize = 13.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(38.dp))
        NeonButton("▶  ИГРАТЬ", onPlay)
        Spacer(Modifier.height(14.dp))
        NeonButton("⚙  НАСТРОЙКИ", onSettings, Panel)
        Spacer(Modifier.height(30.dp))
        Text("РЕКОРД: $bestScore", color = NeonGreen, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        Text("Выбери музыку в игре и попадай в её ритм.", color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
    }
}

@Composable
private fun GameScreen(
    sensitivity: Int,
    soundEnabled: Boolean,
    musicUri: Uri?,
    bpm: Int,
    beatOffsetMs: Int,
    onMusicUriChange: (Uri) -> Unit,
    onBpmChange: (Int) -> Unit,
    onBeatOffsetChange: (Int) -> Unit,
    onGameOver: (Int) -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    var playerLane by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf("ВЫБЕРИ МУЗЫКУ И НАЖМИ В РИТМ") }
    var feedbackColor by remember { mutableStateOf(NeonBlue) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var lastFrameTime by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var nextId by remember { mutableIntStateOf(0) }
    var lastSpawnBeat by remember { mutableLongStateOf(-1L) }

    val gameStartedAt = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val pausedDuration = remember { mutableLongStateOf(0L) }
    val pauseStartedAt = remember { mutableLongStateOf(0L) }
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    val latestScore by rememberUpdatedState(score)
    val latestPlayerLane by rememberUpdatedState(playerLane)
    val latestGameOver by rememberUpdatedState(gameOver)
    val latestPaused by rememberUpdatedState(isPaused)
    val latestOnGameOver by rememberUpdatedState(onGameOver)

    val beatDuration = 60_000L / bpm.coerceIn(60, 200)

    var mediaReady by remember(musicUri) { mutableStateOf(false) }
    val mediaPlayer = remember(musicUri, context) {
        musicUri?.let { uri ->
            runCatching {
                MediaPlayer().apply {
                    setOnPreparedListener { mediaReady = true }
                    setOnErrorListener { _, _, _ -> mediaReady = false; true }
                    setDataSource(context, uri)
                    isLooping = true
                    prepareAsync()
                }
            }.getOrNull()
        }
    }
    DisposableEffect(mediaPlayer) {
        onDispose { runCatching { mediaPlayer?.release() } }
    }

    LaunchedEffect(mediaPlayer, mediaReady, isPaused, gameOver) {
        val player = mediaPlayer ?: return@LaunchedEffect
        if (!mediaReady) return@LaunchedEffect
        runCatching {
            if (isPaused || gameOver) {
                if (player.isPlaying) player.pause()
            } else if (!player.isPlaying) {
                player.start()
            }
        }
    }

    val toneGenerator = remember(soundEnabled) {
        if (soundEnabled) runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull() else null
    }
    DisposableEffect(toneGenerator) { onDispose { runCatching { toneGenerator?.release() } } }

    fun playTone(tone: Int, duration: Int = 50) {
        if (soundEnabled) runCatching { toneGenerator?.startTone(tone, duration) }
    }

    fun timelinePositionMs(): Long {
        if (mediaPlayer != null && mediaReady) {
            return runCatching { mediaPlayer.currentPosition.toLong() }.getOrDefault(0L)
        }
        return (SystemClock.elapsedRealtime() - gameStartedAt.longValue - pausedDuration.longValue).coerceAtLeast(0L)
    }

    fun togglePause() {
        if (gameOver) return
        val now = SystemClock.elapsedRealtime()
        if (isPaused) {
            pausedDuration.longValue += (now - pauseStartedAt.longValue).coerceAtLeast(0L)
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
        if (now - lastTapTime < 100L) return
        lastTapTime = now
        val destination = target.coerceIn(0, 2)
        playerLane = if (sensitivity <= 3) {
            when {
                destination > playerLane -> (playerLane + 1).coerceAtMost(2)
                destination < playerLane -> (playerLane - 1).coerceAtLeast(0)
                else -> playerLane
            }
        } else destination

        val position = timelinePositionMs() - beatOffsetMs
        val phase = ((position % beatDuration) + beatDuration) % beatDuration
        val distance = min(phase, beatDuration - phase)
        when {
            distance <= 90L -> {
                feedback = "PERFECT!"
                feedbackColor = Gold
                score += 25 + (combo / 10) * 5
                combo++
                playTone(ToneGenerator.TONE_PROP_ACK)
            }
            distance <= 180L -> {
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
                playTone(ToneGenerator.TONE_PROP_BEEP2, 40)
            }
        }
    }

    LaunchedEffect(gameOver, isPaused, mediaPlayer, mediaReady, bpm, beatOffsetMs) {
        if (gameOver || isPaused) return@LaunchedEffect
        lastFrameTime = SystemClock.elapsedRealtime()
        while (!gameOver && !isPaused) {
            delay(16L)
            val now = SystemClock.elapsedRealtime()
            val delta = ((now - lastFrameTime).coerceIn(1L, 50L)) / 1000f
            lastFrameTime = now
            val speed = (0.95f + latestScore * 0.0015f).coerceAtMost(1.8f)

            // Создаём препятствия на каждом втором бите выбранного трека.
            val trackTime = timelinePositionMs() - beatOffsetMs
            val beatIndex = if (trackTime >= 0L) trackTime / beatDuration else -1L
            if (beatIndex >= 0L && beatIndex % 2L == 0L && beatIndex != lastSpawnBeat) {
                lastSpawnBeat = beatIndex
                nextId++
                val occupied = obstacles.filter { it.y < 0.12f }.map { it.lane }.toSet()
                val free = (0..2).filterNot { it in occupied }
                val lane = if (free.isNotEmpty()) free.random() else Random.nextInt(0, 3)
                obstacles.add(Obstacle(nextId, lane, -0.08f))
            }

            var passed = 0
            var hit = false
            val updated = obstacles.mapNotNull { obstacle ->
                val newY = obstacle.y + speed * delta
                if (obstacle.lane == latestPlayerLane && obstacle.y <= 0.91f && newY >= 0.80f) hit = true
                if (newY > 1.08f) { passed++; null } else obstacle.copy(y = newY)
            }
            obstacles.clear()
            obstacles.addAll(updated)
            if (passed > 0) score += passed * 10
            if (hit && !latestGameOver) {
                gameOver = true
                playTone(ToneGenerator.TONE_PROP_NACK, 180)
                latestOnGameOver(latestScore + passed * 10)
                break
            }
        }
    }

    // Метроном использует позицию аудиотрека, а не независимый таймер.
    LaunchedEffect(gameOver, isPaused, soundEnabled, mediaPlayer, mediaReady, bpm, beatOffsetMs) {
        if (gameOver || isPaused || !soundEnabled) return@LaunchedEffect
        var lastBeat = Long.MIN_VALUE
        while (!gameOver && !isPaused) {
            val trackTime = timelinePositionMs() - beatOffsetMs
            val beatIndex = if (trackTime >= 0L) trackTime / beatDuration else -1L
            if (beatIndex >= 0L && beatIndex != lastBeat) {
                lastBeat = beatIndex
                playTone(ToneGenerator.TONE_PROP_BEEP, 25)
            }
            delay(8L)
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            onMusicUriChange(uri)
        }
    }

    Column(Modifier.fillMaxSize().background(Background).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("✕", color = TextWhite, fontSize = 25.sp, modifier = Modifier.clickable(onClick = onExit).padding(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BEAT CONTROL", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("$bpm BPM • COMBO x$combo", color = NeonPink, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("СЧЁТ", color = Muted, fontSize = 11.sp)
                Text(score.toString(), color = NeonGreen, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(6.dp))
        NeonButton(if (isPaused) "▶ ПРОДОЛЖИТЬ" else "Ⅱ ПАУЗА", { togglePause() }, Panel)
        Spacer(Modifier.height(6.dp))
        NeonButton(if (musicUri == null) "♫ ВЫБРАТЬ МУЗЫКУ" else "♫ СМЕНИТЬ ТРЕК", { audioPicker.launch(arrayOf("audio/*")) }, Panel)
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                musicUri == null -> "Музыка не выбрана: используется внутренний таймер."
                mediaReady -> "ТРЕК ВОСПРОИЗВОДИТСЯ • настрой BPM и синхронизацию ниже"
                else -> "Загрузка аудио… если не началось, выбери другой файл."
            },
            color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("BPM $bpm", color = TextWhite, fontSize = 12.sp)
            Slider(value = bpm.toFloat(), onValueChange = onBpmChange, valueRange = 60f..200f, steps = 139, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сдвиг ${beatOffsetMs}ms", color = TextWhite, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("−", color = NeonBlue, fontSize = 22.sp, modifier = Modifier.clickable { onBeatOffsetChange(beatOffsetMs - 50) }.padding(horizontal = 12.dp))
            Text("+", color = NeonBlue, fontSize = 22.sp, modifier = Modifier.clickable { onBeatOffsetChange(beatOffsetMs + 50) }.padding(horizontal = 12.dp))
        }
        Text(feedback, color = feedbackColor, fontWeight = FontWeight.Black, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))

        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF0B1020), RoundedCornerShape(18.dp))) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(gameOver, isPaused, sensitivity, bpm, beatOffsetMs) {
                    detectTapGestures { offset ->
                        val laneWidth = size.width / 3f
                        moveToLane((offset.x / laneWidth).toInt().coerceIn(0, 2))
                    }
                }
            ) {
                val laneWidth = size.width / 3f
                val h = size.height
                for (i in 1..2) {
                    val x = laneWidth * i
                    drawLine(NeonBlue.copy(alpha = 0.30f), Offset(x, 0f), Offset(x, h), strokeWidth = 2.dp.toPx())
                }
                drawLine(NeonPink.copy(alpha = 0.65f), Offset(0f, h * 0.87f), Offset(size.width, h * 0.87f), strokeWidth = 2.dp.toPx())
                val px = laneWidth * (playerLane + 0.5f)
                val py = h * 0.86f
                drawCircle(NeonBlue.copy(alpha = 0.18f), 34.dp.toPx(), Offset(px, py))
                drawCircle(NeonBlue, 17.dp.toPx(), Offset(px, py))
                drawCircle(Color.White, 5.dp.toPx(), Offset(px, py))
                obstacles.forEach { obstacle ->
                    val x = laneWidth * (obstacle.lane + 0.5f)
                    val y = h * obstacle.y
                    drawRoundRect(NeonPink, Offset(x - laneWidth * 0.22f, y - 15.dp.toPx()), Size(laneWidth * 0.44f, 30.dp.toPx()), CornerRadius(8.dp.toPx()))
                    drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(x - laneWidth * 0.17f, y - 10.dp.toPx()), Size(laneWidth * 0.34f, 4.dp.toPx()), CornerRadius(3.dp.toPx()))
                }
            }
            Text("НАЖИМАЙ НА ДОРОЖКУ", color = Muted.copy(alpha = 0.75f), fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp))
            if (isPaused && !gameOver) {
                Column(
                    Modifier.align(Alignment.Center).background(Panel.copy(alpha = 0.98f), RoundedCornerShape(18.dp)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ПАУЗА", color = NeonBlue, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(14.dp))
                    NeonButton("▶ ПРОДОЛЖИТЬ", { togglePause() })
                    Spacer(Modifier.height(10.dp))
                    NeonButton("В ГЛАВНОЕ МЕНЮ", onExit, Background)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for (lane in 0..2) {
                Text(
                    when (lane) { 0 -> "◀ ЛЕВО"; 1 -> "● ЦЕНТР"; else -> "ПРАВО ▶" },
                    color = if (playerLane == lane) NeonBlue else Muted,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).clickable { moveToLane(lane) }.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ResultsScreen(score: Int, bestScore: Int, onRetry: () -> Unit, onHome: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("GAME OVER", fontSize = 36.sp, fontWeight = FontWeight.Black, color = NeonPink)
        Spacer(Modifier.height(28.dp))
        Text("ТВОЙ СЧЁТ", color = Muted, fontSize = 14.sp)
        Text(score.toString(), color = NeonBlue, fontSize = 58.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Text("РЕКОРД: $bestScore", color = NeonGreen, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(36.dp))
        NeonButton("↻  ЕЩЁ РАЗ", onRetry)
        Spacer(Modifier.height(14.dp))
        NeonButton("⌂  ГЛАВНОЕ МЕНЮ", onHome, Panel)
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
    Column(Modifier.fillMaxSize().background(Background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("НАСТРОЙКИ", color = NeonBlue, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(32.dp))
        Text("Звук: ${if (soundEnabled) "ВКЛ" else "ВЫКЛ"}", color = TextWhite, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        NeonButton(if (soundEnabled) "ВЫКЛЮЧИТЬ ЗВУК" else "ВКЛЮЧИТЬ ЗВУК", { onSoundChange(!soundEnabled) }, Panel)
        Spacer(Modifier.height(28.dp))
        Text("Чувствительность: $sensitivity", color = TextWhite, fontSize = 18.sp)
        Slider(value = sensitivity.toFloat(), onValueChange = { onSensitivityChange(it.toInt().coerceIn(1, 10)) }, valueRange = 1f..10f, steps = 8, modifier = Modifier.fillMaxWidth())
        Text("При низкой чувствительности игрок перемещается только на одну дорожку.", color = Muted, fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        NeonButton("НАЗАД", onBack)
    }
}

@Composable
private fun NeonButton(text: String, onClick: () -> Unit, color: Color = NeonBlue) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = if (color == NeonBlue) Background else TextWhite)
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
