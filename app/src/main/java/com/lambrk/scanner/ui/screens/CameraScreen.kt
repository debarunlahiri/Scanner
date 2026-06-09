package com.lambrk.scanner.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.lambrk.scanner.ui.components.ConfigureSystemBars
import com.lambrk.scanner.ui.components.QrCodeAnalyzer
import com.lambrk.scanner.ui.navigation.Screen
import com.lambrk.scanner.ui.theme.ScannerTheme
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import java.util.concurrent.Executors

@Composable
fun CameraScreen(navController: NavController) {
    ConfigureSystemBars(
        statusBarColor = Color.Black,
        lightIcons = true
    )

    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var hasNavigated by remember { mutableStateOf(false) }
    var palletId by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraPreview(
                onQrDetected = { code ->
                    if (!hasNavigated) {
                        hasNavigated = true
                        val encoded = java.net.URLEncoder.encode(code, "UTF-8")
                        navController.navigate(Screen.Result.createRoute(encoded)) {
                            popUpTo(Screen.Camera.route) { inclusive = true }
                        }
                    }
                }
            )

            ScanOverlay(
                palletId = palletId,
                onPalletIdChange = { palletId = it },
                onSubmit = { id ->
                    if (id.isNotBlank() && !hasNavigated) {
                        hasNavigated = true
                        val encoded = java.net.URLEncoder.encode(id.trim(), "UTF-8")
                        navController.navigate(Screen.Result.createRoute(encoded)) {
                            popUpTo(Screen.Camera.route) { inclusive = true }
                        }
                    }
                }
            )
        } else {
            PermissionDeniedContent(onRequest = {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            })
        }
    }
}

@Composable
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, QrCodeAnalyzer(onQrDetected))
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanOverlay(
    palletId: String,
    onPalletIdChange: (String) -> Unit,
    onSubmit: (String) -> Unit
) {
    val scanSize = 260.dp
    val focusManager = LocalFocusManager.current

    // Scanning frame
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(scanSize)
                .clip(RoundedCornerShape(20.dp))
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "scan")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = scanSize.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "line"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .offset(y = offsetY.dp)
                    .padding(horizontal = 8.dp)
                    .background(MaterialTheme.colorScheme.secondary)
            )
        }
    }

    // Bottom area: hint text + card input
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding() + 24.dp,
                    top = 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hint label
            Text(
                text = "Align QR code within the frame",
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Divider with "or" helper text
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                Text(
                    text = "  OR  ",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bin ID helper text
            Text(
                text = "Cannot scan QR code? Enter Bin ID below",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Card input row
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.12f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = palletId,
                        onValueChange = onPalletIdChange,
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                text = "Pallet ID",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onSubmit(palletId)
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSubmit(palletId)
                        },
                        enabled = palletId.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                            disabledContainerColor = Color.White.copy(alpha = 0.2f),
                            disabledContentColor = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(width = 80.dp, height = 56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Submit",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Camera permission is required to scan QR codes.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequest) {
            Text("Grant Permission")
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────────────────

@ComposePreview(name = "Camera Screen - Light", showSystemUi = true, showBackground = true)
@Composable
private fun CameraScreenLightPreview() {
    ScannerTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B2838),
                            Color(0xFF2D3E50),
                            Color(0xFF1A2535),
                            Color(0xFF0D1B2A)
                        )
                    )
                )
        ) {
            // Warm room tint to simulate a lit indoor environment
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x22C8A882))
            )
            var palletId by remember { mutableStateOf("") }
            ScanOverlay(
                palletId = palletId,
                onPalletIdChange = { palletId = it },
                onSubmit = {}
            )
        }
    }
}

@ComposePreview(name = "Camera Screen - Dark", showSystemUi = true, showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun CameraScreenDarkPreview() {
    ScannerTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF050D15),
                            Color(0xFF0A1622),
                            Color(0xFF0D1B2A),
                            Color(0xFF060E16)
                        )
                    )
                )
        ) {
            var palletId by remember { mutableStateOf("") }
            ScanOverlay(
                palletId = palletId,
                onPalletIdChange = { palletId = it },
                onSubmit = {}
            )
        }
    }
}

@ComposePreview(name = "Camera Screen - Input Filled", showSystemUi = true)
@Composable
private fun CameraScreenFilledPreview() {
    ScannerTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B2838),
                            Color(0xFF2D3E50),
                            Color(0xFF1A2535)
                        )
                    )
                )
        ) {
            ScanOverlay(
                palletId = "BIN-20481",
                onPalletIdChange = {},
                onSubmit = {}
            )
        }
    }
}
