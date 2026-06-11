package com.lambrk.scanner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lambrk.scanner.ui.navigation.Screen
import com.lambrk.scanner.utils.ExcelOpenStore
import com.lambrk.scanner.utils.GeneratedExcelRegistry
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratedExcelFilesScreen(navController: NavController) {
    val context = LocalContext.current
    val files = remember { GeneratedExcelRegistry.list(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generated Excel Files", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (files.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("No generated Excel files yet.", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(files) { file ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ExcelOpenStore.pendingUri = file.uri
                                navController.navigate(Screen.ExcelViewer.route)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(file.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            DateFormat.getDateTimeInstance().format(Date(file.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Divider()
                }
            }
        }
    }
}
