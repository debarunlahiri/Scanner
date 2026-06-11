package com.lambrk.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lambrk.scanner.data.model.TableData
import com.lambrk.scanner.data.model.TableRow
import com.lambrk.scanner.ui.components.ConfigureSystemBars
import com.lambrk.scanner.ui.navigation.Screen
import com.lambrk.scanner.ui.theme.ScannerTheme
import com.lambrk.scanner.ui.viewmodel.ResultUiState
import com.lambrk.scanner.ui.viewmodel.ResultViewModel
import com.lambrk.scanner.utils.XlsxExporter

// ─── Column definitions ────────────────────────────────────────────────────────────
// Each entry is a column header paired with a lambda that reads the field from TableRow
private val TABLE_COLUMNS: List<Pair<String, (TableRow) -> String>> = listOf(
    "Pallet No"    to { r -> r.palletNo },
    "Bin ID"       to { r -> r.binId },
    "Product Code" to { r -> r.productCode },
    "Qty"          to { r -> r.qty },
    "Date"         to { r -> r.date },
    "H Reason"     to { r -> r.hReason },
    "Grade"        to { r -> r.grade }
)

// ─── Preview helpers ────────────────────────────────────────────────────────────
private fun buildPreviewTableData() = TableData(
    rows = listOf(
        TableRow("Z001A2B3C", "G06B-03", "PC-101", "12", "2026-06-11", "Hold", "A1"),
        TableRow("M00AF12C3", "G06B-03", "PC-102", "5", "2026-06-11", "", "S1"),
        TableRow("Z00BC34D5", "G06B-03", "PC-103", "30", "2026-06-11", "Damage", "A1")
    )
)

private fun TableData.availableRowCount() = rows.count { it.hasApiData() }

private fun TableRow.hasApiData(): Boolean {
    return productCode.isNotBlank() ||
        qty.isNotBlank() ||
        date.isNotBlank() ||
        hReason.isNotBlank() ||
        grade.isNotBlank()
}

private fun TableRow.copyValue(): String {
    return listOf(palletNo, binId, productCode, qty, date, hReason, grade).joinToString("\t")
}

// ─── Dimensions ────────────────────────────────────────────────────────────────
private val CELL_W: Dp    = 160.dp
private val CELL_H: Dp    = 52.dp
private val HDR_H: Dp     = 50.dp
private val IDX_W: Dp     = 46.dp
private val CHK_W: Dp     = 46.dp
private val ACT_W: Dp     = 70.dp

// ─── Theme-adaptive colours ────────────────────────────────────────────────────
private data class TableColors(
    val screenBg: Color,
    val headerBg: Color,
    val headerTxt: Color,
    val rowEven: Color,
    val rowOdd: Color,
    val cellBorder: Color,
    val indexGrey: Color,
    val cellText: Color
)

@Composable
private fun tableColors(): TableColors {
    val dark = isSystemInDarkTheme()
    return if (dark) TableColors(
        screenBg      = Color(0xFF0D1117),
        headerBg      = Color(0xFF0D1B2A),
        headerTxt     = Color(0xFFD0E4FF),
        rowEven       = Color(0xFF1A2232),
        rowOdd        = Color(0xFF151D2B),
        cellBorder    = Color(0xFF2A3A50),
        indexGrey     = Color(0xFF546E7A),
        cellText      = Color(0xFFCDD8E8)
    ) else TableColors(
        screenBg      = Color(0xFFEDF1FB),
        headerBg      = Color(0xFF1A2744),
        headerTxt     = Color(0xFFE8F0FE),
        rowEven       = Color(0xFFF5F8FF),
        rowOdd        = Color(0xFFEBEFF9),
        cellBorder    = Color(0xFFCDD5E0),
        indexGrey     = Color(0xFF78909C),
        cellText      = Color(0xFF1A1A2E)
    )
}


// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController, qrData: String) {
    val isDark = isSystemInDarkTheme()
    val tc = tableColors()
    // Use same navy-blue as table header — unifies AppBar with the table
    val appBarContainer = tc.headerBg
    val appBarContent   = tc.headerTxt
    ConfigureSystemBars(
        statusBarColor = appBarContainer,
        lightIcons = true   // navy is always dark enough for white icons
    )

    val viewModel: ResultViewModel = viewModel { ResultViewModel(qrData) }
    val state = viewModel.uiState.value
    val context = LocalContext.current
    var showExcelViewer by remember { mutableStateOf(false) }

    if (showExcelViewer && state is ResultUiState.Success) {
        ExcelViewerDialog(
            tableData = state.tableData,
            onDismiss = { showExcelViewer = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Results", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate(Screen.Camera.route) {
                            popUpTo(Screen.Result.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state is ResultUiState.Success) {
                        Button(onClick = { navController.navigate(Screen.ExcelFiles.route) }) {
                            Text("Files")
                        }
                        IconButton(onClick = { showExcelViewer = true }) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = "View Excel",
                                tint = appBarContent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = {
                            try {
                                XlsxExporter.saveToDownloads(
                                    context = context,
                                    tableData = state.tableData,
                                    fileName = "pallet_${state.qrCode.take(8)}.xlsx"
                                )
                                Toast.makeText(context, "Excel saved to Downloads", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Filled.FileDownload,
                                contentDescription = "Download Excel",
                                tint = appBarContent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = appBarContainer,
                    titleContentColor = appBarContent,
                    navigationIconContentColor = appBarContent,
                    actionIconContentColor = appBarContent
                )
            )
        },
        containerColor = tableColors().screenBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state) {
                is ResultUiState.Loading -> LoadingContent()
                is ResultUiState.Error   -> ErrorContent(state.message) { viewModel.retry() }
                is ResultUiState.Success -> TableContent(state, context)
            }
        }
    }
}

// ─── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    val isDark = isSystemInDarkTheme()
    val spinnerColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary
    val textColor    = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = spinnerColor, strokeWidth = 4.dp)
        Spacer(Modifier.height(16.dp))
        Text("Loading table…", style = MaterialTheme.typography.titleMedium, color = textColor)
    }
}

// ─── Error ─────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Something went wrong", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(10.dp)) { Text("Retry") }
    }
}

// ─── Table content ─────────────────────────────────────────────────────────────

@Composable
private fun TableContent(state: ResultUiState.Success, context: Context) {
    var rawScale by remember { mutableFloatStateOf(1f) }
    val scale by animateFloatAsState(rawScale.coerceIn(0.4f, 3f), label = "scale")
    val tc = tableColors()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedPalletNos by remember { mutableStateOf(setOf<String>()) }
    val selectablePalletNos = state.tableData.rows
        .filter { it.hasApiData() && it.palletNo.isNotBlank() }
        .map { it.palletNo }
    val allSelected = selectablePalletNos.isNotEmpty() && selectedPalletNos.size == selectablePalletNos.size

    Column(Modifier.fillMaxSize()) {
        // ── Summary banner ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(tc.headerBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📦  ${state.tableData.availableRowCount()} available  •  ${TABLE_COLUMNS.size} columns",
                color = tc.headerTxt,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                Button(onClick = {
                    selectionMode = !selectionMode
                    if (!selectionMode) selectedPalletNos = emptySet()
                }) {
                    Text(if (selectionMode) "Done" else "Select")
                }
            }
        }

        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tc.headerBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${selectedPalletNos.size} selected",
                    color = tc.headerTxt,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        selectedPalletNos = if (allSelected) emptySet() else selectablePalletNos.toSet()
                    }) {
                        Text(if (allSelected) "Deselect All" else "Select All")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            copyText(context, selectedPalletNos.sorted().joinToString(", "))
                        },
                        enabled = selectedPalletNos.isNotEmpty()
                    ) {
                        Text("Copy")
                    }
                }
            }
        }

        // ── Zoomable table ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        rawScale = (rawScale * zoom).coerceIn(0.4f, 3f)
                    }
                }
        ) {
            Box(modifier = Modifier.scale(scale)) {
                TableGrid(
                    tableData = state.tableData,
                    tc = tc,
                    selectionMode = selectionMode,
                    selectedPalletNos = selectedPalletNos,
                    allSelected = allSelected,
                    onToggleSelectAll = {
                        selectedPalletNos = if (allSelected) emptySet() else selectablePalletNos.toSet()
                    },
                    onTogglePalletNo = { palletNo ->
                        selectedPalletNos = if (selectedPalletNos.contains(palletNo)) {
                            selectedPalletNos - palletNo
                        } else {
                            selectedPalletNos + palletNo
                        }
                    },
                    onCopyPalletNo = { palletNo -> copyText(context, palletNo) },
                    onCopyRow = { row -> copyText(context, row.copyValue()) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcelViewerDialog(
    tableData: TableData,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ExcelViewerScaffold(
            title = "Excel Viewer",
            tableData = tableData,
            onClose = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcelViewerScaffold(
    title: String,
    tableData: TableData,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val tc = tableColors()
    ConfigureSystemBars(
        statusBarColor = tc.headerBg,
        lightIcons = tc.headerBg.luminance() < 0.5f
    )
    var rawScale by remember { mutableFloatStateOf(1f) }
    val scale by animateFloatAsState(rawScale.coerceIn(0.4f, 3f), label = "viewerScale")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = tc.screenBg
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.headerBg,
                    titleContentColor = tc.headerTxt,
                    navigationIconContentColor = tc.headerTxt
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tc.headerBg)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📦  ${tableData.availableRowCount()} available  •  ${TABLE_COLUMNS.size} columns",
                    color = tc.headerTxt,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            rawScale = (rawScale * zoom).coerceIn(0.4f, 3f)
                        }
                    }
            ) {
                Box(modifier = Modifier.scale(scale)) {
                    TableGrid(
                        tableData = tableData,
                        tc = tc,
                        selectionMode = false,
                        selectedPalletNos = emptySet(),
                        allSelected = false,
                        onToggleSelectAll = {},
                        onTogglePalletNo = {},
                        onCopyPalletNo = { palletNo -> copyText(context, palletNo) },
                        onCopyRow = { row -> copyText(context, row.copyValue()) }
                    )
                }
            }
        }
    }
}

// ─── Grid ──────────────────────────────────────────────────────────────────────

@Composable
private fun TableGrid(
    tableData: TableData,
    tc: TableColors,
    selectionMode: Boolean,
    selectedPalletNos: Set<String>,
    allSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onTogglePalletNo: (String) -> Unit,
    onCopyPalletNo: (String) -> Unit,
    onCopyRow: (TableRow) -> Unit
) {
    val hScroll = rememberScrollState()

    Column {
        // ── Header row ──
        Row(
            modifier = Modifier
                .horizontalScroll(hScroll)
                .background(tc.headerBg)
        ) {
            if (selectionMode) {
                CheckboxCell(
                    checked = allSelected,
                    enabled = tableData.rows.any { it.hasApiData() && it.palletNo.isNotBlank() },
                    tc = tc,
                    height = HDR_H,
                    onClick = onToggleSelectAll
                )
            }
            HeaderCell(text = "#", width = IDX_W, tc = tc)
            TABLE_COLUMNS.forEach { (header, _) -> HeaderCell(text = header, width = CELL_W, tc = tc) }
            HeaderCell(text = "Copy", width = ACT_W, tc = tc)
        }

        // ── Data rows ──
        LazyColumn {
            itemsIndexed(tableData.rows) { idx, row ->
                DataRow(
                    index = idx + 1,
                    row = row,
                    isEven = idx % 2 == 0,
                    tc = tc,
                    hScroll = hScroll,
                    selectionMode = selectionMode,
                    isSelected = selectedPalletNos.contains(row.palletNo),
                    onTogglePalletNo = { onTogglePalletNo(row.palletNo) },
                    onCopyPalletNo = { onCopyPalletNo(row.palletNo) },
                    onCopyRow = { onCopyRow(row) }
                )
            }
        }
    }
}

// ─── Header cells ──────────────────────────────────────────────────────────────

@Composable
private fun HeaderCell(text: String, width: Dp, tc: TableColors) {
    Box(
        modifier = Modifier
            .width(width)
            .height(HDR_H)
            .border(0.5.dp, tc.headerTxt.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = tc.headerTxt,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
    }
}

@Composable
private fun CheckboxCell(
    checked: Boolean,
    enabled: Boolean,
    tc: TableColors,
    height: Dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(CHK_W)
            .height(height)
            .border(0.5.dp, tc.cellBorder)
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (enabled) {
            Icon(
                imageVector = if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ─── Data row ──────────────────────────────────────────────────────────────────

@Composable
private fun DataRow(
    index: Int,
    row: TableRow,
    isEven: Boolean,
    tc: TableColors,
    hScroll: ScrollState,
    selectionMode: Boolean,
    isSelected: Boolean,
    onTogglePalletNo: () -> Unit,
    onCopyPalletNo: () -> Unit,
    onCopyRow: () -> Unit
) {
    val bg = if (isEven) tc.rowEven else tc.rowOdd
    val hasApiData = row.hasApiData()

    Row(
        modifier = Modifier
            .horizontalScroll(hScroll)
            .background(bg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            CheckboxCell(
                checked = isSelected,
                enabled = hasApiData && row.palletNo.isNotBlank(),
                tc = tc,
                height = CELL_H,
                onClick = onTogglePalletNo
            )
        }

        Box(
            modifier = Modifier
                .width(IDX_W)
                .height(CELL_H)
                .border(0.5.dp, tc.cellBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "$index", fontSize = 11.sp, color = tc.indexGrey, fontWeight = FontWeight.Medium)
        }

        // ── Data cells ──
        TABLE_COLUMNS.forEachIndexed { colIdx, (_, accessor) ->
            val value = if (hasApiData) accessor(row) else ""
            Box(
                modifier = Modifier
                    .width(CELL_W)
                    .height(CELL_H)
                    .border(0.5.dp, tc.cellBorder),
                contentAlignment = Alignment.CenterStart
            ) {
                if (colIdx == 0 && hasApiData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                    ) {
                        Text(
                            text = value,
                            fontSize = 12.sp,
                            color = tc.cellText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onCopyPalletNo, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy pallet no",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        color = tc.cellText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(ACT_W)
                .height(CELL_H)
                .border(0.5.dp, tc.cellBorder),
            contentAlignment = Alignment.Center
        ) {
            if (hasApiData) {
                IconButton(onClick = onCopyRow, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy row",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Row", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(name = "Result Screen – Light", showSystemUi = true)
@Composable
private fun ResultScreenLightPreview() {
    ScannerTheme(darkTheme = false) {
        Surface {
            val mockTable = buildPreviewTableData()
            val tc = tableColors()
            Column(Modifier.fillMaxSize().background(tc.screenBg)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(tc.headerBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📦  ${mockTable.availableRowCount()} available  •  ${TABLE_COLUMNS.size} columns",
                        color = tc.headerTxt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
                }
                TableGrid(
                    tableData = mockTable,
                    tc = tc,
                    selectionMode = false,
                    selectedPalletNos = emptySet(),
                    allSelected = false,
                    onToggleSelectAll = {},
                    onTogglePalletNo = {},
                    onCopyPalletNo = {},
                    onCopyRow = {}
                )
            }
        }
    }
}

@Preview(name = "Result Screen – Dark", showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun ResultScreenDarkPreview() {
    ScannerTheme(darkTheme = true) {
        Surface {
            val mockTable = buildPreviewTableData()
            val tc = tableColors()
            Column(Modifier.fillMaxSize().background(tc.screenBg)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(tc.headerBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📦  ${mockTable.availableRowCount()} available  •  ${TABLE_COLUMNS.size} columns",
                        color = tc.headerTxt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
                }
                TableGrid(
                    tableData = mockTable,
                    tc = tc,
                    selectionMode = false,
                    selectedPalletNos = emptySet(),
                    allSelected = false,
                    onToggleSelectAll = {},
                    onTogglePalletNo = {},
                    onCopyPalletNo = {},
                    onCopyRow = {}
                )
            }
        }
    }
}
