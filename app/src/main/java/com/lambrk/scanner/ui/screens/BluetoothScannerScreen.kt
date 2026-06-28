package com.lambrk.scanner.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lambrk.scanner.ui.theme.ScannerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

const val BLUETOOTH_SCAN_RESULT_KEY = "bluetooth_scan_result"

class BluetoothScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScannerTheme {
                BluetoothScannerContent(
                    onBack = { finish() },
                    onScanned = { value ->
                        setResult(
                            Activity.RESULT_OK,
                            Intent().putExtra(BLUETOOTH_SCAN_RESULT_KEY, value)
                        )
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothScannerContent(
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasBluetoothPermission by remember { mutableStateOf(hasBluetoothPermissions(context)) }
    var devices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    var status by remember { mutableStateOf("Select a paired Bluetooth scanner.") }
    var connectedDeviceName by remember { mutableStateOf<String?>(null) }
    var connectingDeviceAddress by remember { mutableStateOf<String?>(null) }
    var deviceConnectionMessages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var bluetoothSocket by remember { mutableStateOf<BluetoothSocket?>(null) }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasBluetoothPermission = permissions.values.all { it } || hasBluetoothPermissions(context)
        if (hasBluetoothPermission) {
            devices = getPairedBluetoothDevices(context)
            status = "Select a paired Bluetooth scanner."
        } else {
            status = "Allow Bluetooth permission to connect a scanner."
        }
    }

    fun refreshDevices() {
        if (hasBluetoothPermissions(context)) {
            hasBluetoothPermission = true
            devices = getPairedBluetoothDevices(context)
            status = "Select a paired Bluetooth scanner."
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(BLUETOOTH_RUNTIME_PERMISSIONS)
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        connectingDeviceAddress = device.address
        deviceConnectionMessages = deviceConnectionMessages + (device.address to "Connecting...")
        coroutineScope.launch {
            bluetoothSocket?.closeQuietly()
            val result = connectToBluetoothScanner(context, device)
            result
                .onSuccess { socket ->
                    connectingDeviceAddress = null
                    bluetoothSocket = socket
                    connectedDeviceName = device.safeName()
                    deviceConnectionMessages = mapOf(device.address to "Connected. Scan a barcode.")
                    launch {
                        readBluetoothScanner(socket) { scannedValue ->
                            onScanned(scannedValue)
                        }
                    }
                }
                .onFailure { error ->
                    connectingDeviceAddress = null
                    bluetoothSocket = null
                    connectedDeviceName = null
                    deviceConnectionMessages = deviceConnectionMessages + (
                            device.address to bluetoothErrorMessage(error)
                            )
                }
        }
    }

    LaunchedEffect(Unit) {
        refreshDevices()
    }

    DisposableEffect(bluetoothSocket) {
        onDispose {
            bluetoothSocket?.closeQuietly()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bluetooth scanner") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = ::refreshDevices) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Bluetooth devices"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (!hasBluetoothPermission) {
                PermissionRequiredContent(onRequest = ::refreshDevices)
            } else if (devices.isEmpty()) {
                EmptyBluetoothContent()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(devices) { device ->
                        BluetoothDeviceRow(
                            device = device,
                            isConnected = device.safeName() == connectedDeviceName,
                            isConnecting = device.address == connectingDeviceAddress,
                            message = deviceConnectionMessages[device.address],
                            onClick = { connectDevice(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(onRequest: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Bluetooth permission is required to connect a scanner.",
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRequest) {
                Text("Allow Bluetooth")
            }
        }
    }
}

@Composable
private fun EmptyBluetoothContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No paired Bluetooth devices found. Pair the scanner in Android Bluetooth settings first.",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BluetoothDeviceRow(
    device: BluetoothDevice,
    isConnected: Boolean,
    isConnecting: Boolean,
    message: String?,
    onClick: () -> Unit
) {
    val status = when {
        isConnecting -> "Connecting..."
        isConnected -> "Connected. Scan a barcode."
        else -> message ?: "Paired Bluetooth device"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = device.safeName(),
                    fontWeight = FontWeight.SemiBold
                )
            },
            supportingContent = { Text(status) },
            trailingContent = {
                if (isConnected || isConnecting) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

private fun hasBluetoothPermissions(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            BLUETOOTH_RUNTIME_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
}

private fun hasBluetoothScanPermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
}

@Suppress("MissingPermission")
private fun getPairedBluetoothDevices(context: Context): List<BluetoothDevice> {
    if (!hasBluetoothPermissions(context)) return emptyList()
    val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    val adapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    return adapter?.bondedDevices
        ?.sortedBy { it.safeName().lowercase() }
        .orEmpty()
}

@Suppress("MissingPermission")
private suspend fun connectToBluetoothScanner(
    context: Context,
    device: BluetoothDevice
): Result<BluetoothSocket> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: error("Bluetooth is not available on this device.")
            if (!adapter.isEnabled) {
                error("Turn on Bluetooth to connect a scanner.")
            }
            if (hasBluetoothScanPermission(context)) {
                adapter.cancelDiscovery()
            }
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            socket
        }
    }
}

private suspend fun readBluetoothScanner(
    socket: BluetoothSocket,
    onScanned: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        val buffer = ByteArray(256)
        val line = StringBuilder()
        while (socket.isConnected) {
            val count = try {
                socket.inputStream.read(buffer)
            } catch (_: IOException) {
                break
            }
            if (count <= 0) break
            buffer.decodeToString(endIndex = count).forEach { char ->
                if (char == '\n' || char == '\r') {
                    val value = line.toString().trim()
                    line.clear()
                    if (value.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            onScanned(value)
                        }
                    }
                } else {
                    line.append(char)
                }
            }
        }
    }
}

@Suppress("MissingPermission")
private fun BluetoothDevice.safeName(): String {
    return name?.takeIf { it.isNotBlank() } ?: "Unnamed device"
}

private fun bluetoothErrorMessage(error: Throwable): String {
    return when (error) {
        is SecurityException -> "Allow Bluetooth permission to connect a scanner."
        is IOException -> "Could not connect. Make sure the scanner is on and paired."
        else -> error.message?.takeIf { it.length < 90 }
            ?: "Could not connect to the scanner."
    }
}

private fun BluetoothSocket.closeQuietly() {
    try {
        close()
    } catch (_: IOException) {
    }
}

private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private val BLUETOOTH_RUNTIME_PERMISSIONS = arrayOf(
    Manifest.permission.BLUETOOTH_CONNECT,
    Manifest.permission.BLUETOOTH_SCAN
)
