package com.lambrk.scanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lambrk.scanner.ui.screens.CameraScreen
import com.lambrk.scanner.ui.screens.ResultScreen
import com.lambrk.scanner.ui.screens.SplashScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Camera : Screen("camera")
    data object Result : Screen("result/{qrData}") {
        fun createRoute(qrData: String) = "result/$qrData"
    }
}

@Composable
fun ScannerNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Camera.route) {
            CameraScreen(navController = navController)
        }
        composable(
            route = Screen.Result.route,
            arguments = listOf(navArgument("qrData") { type = NavType.StringType })
        ) { backStackEntry ->
            val qrData = backStackEntry.arguments?.getString("qrData").orEmpty()
            ResultScreen(navController = navController, qrData = qrData)
        }
    }
}
