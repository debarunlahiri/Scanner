package com.lambrk.scanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.lambrk.scanner.ui.navigation.ScannerNavHost
import com.lambrk.scanner.ui.navigation.Screen
import com.lambrk.scanner.ui.theme.ScannerTheme
import com.lambrk.scanner.utils.ExcelOpenStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleExcelOpenIntent(intent)
        enableEdgeToEdge()
        setContent {
            ScannerTheme {
                val navController = rememberNavController()
                ScannerNavHost(
                    navController = navController,
                    startDestination = if (ExcelOpenStore.pendingUri != null) {
                        Screen.ExcelViewer.route
                    } else {
                        Screen.Splash.route
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExcelOpenIntent(intent)
        if (ExcelOpenStore.pendingUri != null) recreate()
    }

    private fun handleExcelOpenIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri: Uri = intent.data ?: return
        ExcelOpenStore.pendingUri = uri
    }
}
