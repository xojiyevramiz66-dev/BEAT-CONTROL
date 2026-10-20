package com.beatcontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00F0FF),
                    secondary = Color(0xFFFF007F),
                    background = Color(0xFF0A0A12),
                    surface = Color(0xFF141423)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Game : Screen("game")
    object Settings : Screen("settings")
    object Results : Screen("results")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.MainMenu.route) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onPlayClick = { navController.navigate(Screen.Game.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Game.route) {
            GameScreen(
                onGameOver = { navController.navigate(Screen.Results.route) },
                onPauseClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Results.route) {
            ResultsScreen(
                onRestart = { 
                    navController.popBackStack(Screen.MainMenu.route, inclusive = false)
                    navController.navigate(Screen.Game.route)
                },
                onHome = { navController.popBackStack(Screen.MainMenu.route, inclusive = false) }
            )
        }
    }
}

@Composable
fun MainMenuScreen(onPlayClick: () -> Unit, onSettingsClick: () -> Unit) {
    val darkBackground = Color(0xFF0A0A12)
    val neonCyan = Color(0xFF00F0FF)
    val neonPink = Color(0xFFFF007F)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(darkBackground, Color(0xFF1A0033)))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BEAT CONTROL", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = neonCyan)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Управляй ритмом", fontSize = 16.sp, color = neonPink)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onPlayClick,
                modifier = Modifier.width(220.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
            ) {
                Text("ИГРАТЬ", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier.width(220.dp).height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = neonPink)
            ) {
                Text("НАСТРОЙКИ", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun GameScreen(onGameOver: () -> Unit, onPauseClick: () -> Unit) {
    var score by remember { mutableStateOf(0) }
    var playerLane by remember { mutableStateOf(1) }

    val darkBackground = Color(0xFF0A0A12)
    val neonCyan = Color(0xFF00F0FF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBackground)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (offset.x < size.width / 2) {
                        if (playerLane > 0) playerLane--
                    } else {
                        if (playerLane < 2) playerLane++
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val laneWidth = size.width / 3f
            
            for (i in 1..2) {
                drawLine(
                    color = Color.DarkGray.copy(alpha = 0.5f),
                    start = Offset(laneWidth * i, 0f),
                    end = Offset(laneWidth * i, size.height),
                    strokeWidth = 3f
                )
            }

            val playerX = laneWidth * playerLane + laneWidth / 2f
            drawCircle(color = neonCyan, radius = 55f, center = Offset(playerX, size.height - 250f))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("СЧЁТ: $score", color = neonCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = onPauseClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141423))
            ) {
                Text("ПАУЗА", color = Color.White)
            }
        }
    }
}

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

            Text(text = "Громкость музыки: ${(musicVol * 100).toInt()}%", color = Color.White)
            Slider(
                value = musicVol,
                onValueChange = { musicVol = it },
                colors = SliderDefaults.colors(thumbColor = neonCyan, activeTrackColor = neonCyan)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "Громкость эффектов: ${(soundVol * 100).toInt()}%", color = Color.White)
            Slider(
                value = soundVol,
                onValueChange = { soundVol = it },
                colors = SliderDefaults.colors(thumbColor = neonPink, activeTrackColor = neonPink)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
            ) {
                Text(text = "НАЗАД", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}

@Composable
fun ResultsScreen(onRestart: () -> Unit, onHome: () -> Unit) {
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
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = neonCyan)
            ) {
                Text(text = "ПОВТОРИТЬ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onHome,
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = neonPink)
            ) {
                Text(text = "В МЕНЮ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
