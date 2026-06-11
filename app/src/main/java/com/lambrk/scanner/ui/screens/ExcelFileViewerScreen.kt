package com.lambrk.scanner.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.lambrk.scanner.utils.ExcelOpenStore
import com.lambrk.scanner.utils.XlsxExporter

@Composable
fun ExcelFileViewerScreen(navController: NavController) {
    val context = LocalContext.current
    val uri = ExcelOpenStore.pendingUri

    if (uri == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No Excel file selected.")
        }
        return
    }

    val tableData = remember(uri) {
        try {
            Result.success(XlsxExporter.read(context, uri))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open Excel: ${e.message}", Toast.LENGTH_SHORT).show()
            Result.failure(e)
        }
    }

    val data = tableData.getOrNull()
    if (data == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Could not open this Excel file.")
        }
    } else {
        ExcelViewerScaffold(
            title = "Excel Viewer",
            tableData = data,
            onClose = { navController.popBackStack() }
        )
    }
}
