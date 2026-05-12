package com.lambrk.scanner.ui.components

import android.app.Activity
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun ConfigureSystemBars(
    statusBarColor: Color,
    lightIcons: Boolean
) {
    val view = LocalView.current
    DisposableEffect(statusBarColor, lightIcons) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)

        window.statusBarColor = statusBarColor.toArgb()
        insetsController.isAppearanceLightStatusBars = !lightIcons

        onDispose { }
    }
}
