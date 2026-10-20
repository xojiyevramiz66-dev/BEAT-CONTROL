package com.beatcontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.beatcontrol.app.ui.screens.*
import com.beatcontrol.app.ui.theme.BeatControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BeatControlTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Game : Screen("game/{levelId}") {
        fun createRoute(levelId: String) = "game/$levelId"
    }
    object Settings : Screen("settings")
    object Leaderboard : Screen("leaderboard")
    object Results : Screen("results/{score}/{combo}/{accuracy}") {
        fun createRoute(score: Int, combo: Int, accuracy: Float) = "results/$score/$combo/$accuracy"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.MainMenu.route) {
        // Главное меню
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onPlayClick = { levelId -> navController.navigate(Screen.Game.createRoute(levelId)) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onLeaderboardClick = { navController.navigate(Screen.Leaderboard.route) }
            )
        }

        // Игровой экран
        composable(Screen.Game.route) { backStackEntry ->
            val levelId = backStackEntry.arguments?.getString("levelId") ?: "normal"
            GameScreen(
                levelId = levelId,
                onGameOver = { score, combo, accuracy ->
                    navController.navigate(Screen.Results.createRoute(score, combo, accuracy)) {
                        popUpTo(Screen.MainMenu.route)
                    }
                },
                onPauseClick = { navController.popBackStack() }
            )
        }

        // Настройки
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        // Таблица рекордов
        composable(Screen.Leaderboard.route) {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }

        // Экран результатов
        composable(Screen.Results.route) { backStackEntry ->
            val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
            val combo = backStackEntry.arguments?.getString("combo")?.toIntOrNull() ?: 0
            val accuracy = backStackEntry.arguments?.getString("accuracy")?.toFloatOrNull() ?: 0f

            ResultsScreen(
                score = score,
                maxCombo = combo,
                accuracy = accuracy,
                onRestart = { 
                    navController.popBackStack(Screen.MainMenu.route, inclusive = false)
                    navController.navigate(Screen.Game.createRoute("normal"))
                },
                onHome = { navController.popBackStack(Screen.MainMenu.route, inclusive = false) }
            )
        }
    }
}
