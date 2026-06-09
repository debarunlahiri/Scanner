package com.lambrk.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
    "Pallet ID"   to { r -> r.palletId },
    "SKU"         to { r -> r.sku },
    "Description" to { r -> r.description },
    "Qty"         to { r -> r.qty },
    "Weight (kg)" to { r -> r.weight },
    "Location"    to { r -> r.location },
    "Status"      to { r -> r.status },
    "Scan Time"   to { r -> r.scanTime },
    "User ID"     to { r -> r.userId }
)

// ─── Preview helpers ────────────────────────────────────────────────────────────
private fun buildPreviewTableData() = TableData(
    rows = listOf(
        TableRow("PLT-0001", "SKU-101", "Widget A",   "12",  "1.50", "A-01-01", "Received",   "2025-06-01 09:00", "U-1"),
        TableRow("PLT-0002", "SKU-102", "Widget B",    "5",  "3.20", "B-02-03", "In Transit", "2025-06-02 10:15", "U-2"),
        TableRow("PLT-0003", "SKU-103", "Gadget Pro", "30",  "0.80", "C-03-02", "Stored",     "2025-06-03 11:30", "U-1")
    )
)

// ─── Dimensions ────────────────────────────────────────────────────────────────
private val CELL_W: Dp    = 160.dp
private val CELL_H: Dp    = 52.dp
private val HDR_H: Dp     = 50.dp
private val IDX_W: Dp     = 46.dp
private val CHK_W: Dp     = 46.dp
private val ACT_W: Dp     = 70.dp

// AccentOrange removed — accent is now MaterialTheme.colorScheme.secondary (blue)
private val PalletBlue = Color(0xFF1565C0)  // used for pallet chip and checkbox tint

// ─── Theme-adaptive colours ────────────────────────────────────────────────────
private data class TableColors(
    val screenBg: Color,
    val headerBg: Color,
    val headerTxt: Color,
    val rowEven: Color,
    val rowOdd: Color,
    val rowSelected: Color,
    val cellBorder: Color,
    val indexGrey: Color,
    val selectionBarBg: Color,
    val cellText: Color,
    val checkUnchecked: Color
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
        rowSelected   = Color(0xFF0D3460),
        cellBorder    = Color(0xFF2A3A50),
        indexGrey     = Color(0xFF546E7A),
        selectionBarBg= Color(0xFF0A2744),
        cellText      = Color(0xFFCDD8E8),
        checkUnchecked= Color(0xFF455A64)
    ) else TableColors(
        screenBg      = Color(0xFFEDF1FB),
        headerBg      = Color(0xFF1A2744),
        headerTxt     = Color(0xFFE8F0FE),
        rowEven       = Color(0xFFF5F8FF),
        rowOdd        = Color(0xFFEBEFF9),
        rowSelected   = Color(0xFFD0E4FF),
        cellBorder    = Color(0xFFCDD5E0),
        indexGrey     = Color(0xFF78909C),
        selectionBarBg= Color(0xFF0D47A1),
        cellText      = Color(0xFF1A1A2E),
        checkUnchecked= Color(0xFFB0BEC5)
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
                        IconButton(onClick = {
                            try {
                                val uri = XlsxExporter.export(
                                    context = context,
                                    tableData = state.tableData,
                                    fileName = "pallet_${state.qrCode.take(8)}.xlsx"
                                )
                                XlsxExporter.shareExcel(context, uri)
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

    // Multi-select state
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val allIds = state.tableData.rows.map { it.palletId }
    val allSelected = selectedIds.size == allIds.size && allIds.isNotEmpty()

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
                text = "📦  ${state.tableData.rows.size} rows  •  ${TABLE_COLUMNS.size} columns",
                color = tc.headerTxt,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
                Spacer(Modifier.width(12.dp))
                // Select / Done toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selectionMode) MaterialTheme.colorScheme.secondary else Color.White.copy(alpha = 0.15f)
                        )
                        .clickable {
                            selectionMode = !selectionMode
                            if (!selectionMode) selectedIds = emptySet()
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (selectionMode) "Done" else "Select",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // ── Selection action bar (visible when selectionMode + ≥1 selected) ─
        AnimatedVisibility(
            visible = selectionMode && selectedIds.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SelectionBar(
                selectedCount = selectedIds.size,
                totalCount = allIds.size,
                allSelected = allSelected,
                selectionBarBg = tc.selectionBarBg,
                onSelectAll = {
                    selectedIds = if (allSelected) emptySet() else allIds.toSet()
                },
                onCopySelected = {
                    val csv = selectedIds.sorted().joinToString(", ")
                    copyText(context, csv)
                },
                onClearSelection = { selectedIds = emptySet() }
            )
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
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    allSelected = allSelected,
                    tc = tc,
                    onToggleSelectAll = {
                        selectedIds = if (allSelected) emptySet() else allIds.toSet()
                    },
                    onToggleRow = { palletId ->
                        selectedIds = if (selectedIds.contains(palletId)) {
                            selectedIds - palletId
                        } else {
                            selectedIds + palletId
                        }
                    },
                    onCopyRow = { id -> copyText(context, id) },
                    onCopyCell = { v -> copyText(context, v) }
                )
            }
        }
    }
}

// ─── Selection action bar ──────────────────────────────────────────────────────

@Composable
private fun SelectionBar(
    selectedCount: Int,
    totalCount: Int,
    allSelected: Boolean,
    selectionBarBg: Color,
    onSelectAll: () -> Unit,
    onCopySelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(selectionBarBg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: count + clear
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$selectedCount / $totalCount selected",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Clear",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { onClearSelection() }
                    .padding(4.dp)
            )
        }

        // Right: Select All + Copy
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Select All toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelectAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (allSelected) Icons.Filled.CheckBox else Icons.Filled.SelectAll,
                    contentDescription = "Select All",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (allSelected) "Deselect All" else "Select All",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(8.dp))

            // Copy selected (comma-separated)
            Button(
                onClick = onCopySelected,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Copy IDs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Grid ──────────────────────────────────────────────────────────────────────

@Composable
private fun TableGrid(
    tableData: TableData,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    allSelected: Boolean,
    tc: TableColors,
    onToggleSelectAll: () -> Unit,
    onToggleRow: (String) -> Unit,
    onCopyRow: (String) -> Unit,
    onCopyCell: (String) -> Unit
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
                CheckboxHeaderCell(allSelected = allSelected, tc = tc, onToggle = onToggleSelectAll)
            }
            HeaderCell(text = "#", width = IDX_W, tc = tc)
            TABLE_COLUMNS.forEach { (header, _) -> HeaderCell(text = header, width = CELL_W, tc = tc) }
            HeaderCell(text = "Copy ID", width = ACT_W, tc = tc)
        }

        // ── Data rows ──
        LazyColumn {
            itemsIndexed(tableData.rows) { idx, row ->
                DataRow(
                    index = idx + 1,
                    row = row,
                    isEven = idx % 2 == 0,
                    selectionMode = selectionMode,
                    isSelected = selectedIds.contains(row.palletId),
                    tc = tc,
                    hScroll = hScroll,
                    onToggleSelect = { onToggleRow(row.palletId) },
                    onCopyRow = onCopyRow,
                    onCopyCell = onCopyCell
                )
            }
        }
    }
}

// ─── Header cells ──────────────────────────────────────────────────────────────

@Composable
private fun CheckboxHeaderCell(allSelected: Boolean, tc: TableColors, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(CHK_W)
            .height(HDR_H)
            .border(0.5.dp, tc.headerTxt.copy(alpha = 0.2f))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (allSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = "Select All",
            tint = if (allSelected) MaterialTheme.colorScheme.secondary else tc.headerTxt.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
    }
}

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

// ─── Data row ──────────────────────────────────────────────────────────────────

@Composable
private fun DataRow(
    index: Int,
    row: TableRow,
    isEven: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    tc: TableColors,
    hScroll: ScrollState,
    onToggleSelect: () -> Unit,
    onCopyRow: (String) -> Unit,
    onCopyCell: (String) -> Unit
) {
    val bg = when {
        isSelected -> tc.rowSelected
        isEven     -> tc.rowEven
        else       -> tc.rowOdd
    }

    Row(
        modifier = Modifier
            .horizontalScroll(hScroll)
            .background(bg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Checkbox cell ──
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .width(CHK_W)
                    .height(CELL_H)
                    .border(0.5.dp, tc.cellBorder)
                    .clickable { onToggleSelect() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                    contentDescription = "Select row",
                    tint = if (isSelected) PalletBlue else tc.checkUnchecked,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── Row index ──
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
            val value = accessor(row)
            Box(
                modifier = Modifier
                    .width(CELL_W)
                    .height(CELL_H)
                    .border(0.5.dp, tc.cellBorder),
                contentAlignment = Alignment.CenterStart
            ) {
                if (colIdx == 0) { // palletId is the first column
                    PalletChip(
                        palletId = value,
                        isSelected = isSelected,
                        onCopy = { onCopyCell(value) }
                    )
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

        // ── Copy row action ──
        Box(
            modifier = Modifier
                .width(ACT_W)
                .height(CELL_H)
                .border(0.5.dp, tc.cellBorder),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = { onCopyRow(row.palletId) }, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Pallet ID",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Pallet ID chip ────────────────────────────────────────────────────────────

@Composable
private fun PalletChip(palletId: String, isSelected: Boolean, onCopy: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    // Dark mode: bright accent text (readable on dark row bg)
    // Light mode: classic navy blue
    val chipAccent = if (isDark) MaterialTheme.colorScheme.secondary else PalletBlue
    val chipBg     = chipAccent.copy(alpha = if (isSelected) 0.25f else 0.12f)

    Row(
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(chipBg)
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = palletId,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = chipAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 92.dp)
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(chipAccent),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onCopy, modifier = Modifier.size(22.dp)) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

// ─── Clipboard ───────────────────────────────────────────────────────────────────

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Pallet", text))
    val display = if (text.length > 60) text.take(60) + "…" else text
    Toast.makeText(context, "Copied: $display", Toast.LENGTH_SHORT).show()
}

// ─── Previews ───────────────────────────────────────────────────────────────────

@Preview(name = "Result Screen – Light", showSystemUi = true)
@Composable
private fun ResultScreenLightPreview() {
    ScannerTheme(darkTheme = false) {
        Surface {
            val mockTable = buildPreviewTableData()
            val tc = tableColors()
            var selectionMode by remember { mutableStateOf(false) }
            var selectedIds by remember { mutableStateOf(setOf<String>()) }
            val allIds = mockTable.rows.map { it.palletId }
            val allSelected = selectedIds.size == allIds.size && allIds.isNotEmpty()
            Column(Modifier.fillMaxSize().background(tc.screenBg)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(tc.headerBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📦  ${mockTable.rows.size} rows  •  ${TABLE_COLUMNS.size} columns",
                        color = tc.headerTxt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
                }
                TableGrid(
                    tableData = mockTable,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    allSelected = allSelected,
                    tc = tc,
                    onToggleSelectAll = {
                        selectedIds = if (allSelected) emptySet() else allIds.toSet()
                    },
                    onToggleRow = { id ->
                        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                    },
                    onCopyRow = {},
                    onCopyCell = {}
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
            var selectionMode by remember { mutableStateOf(false) }
            var selectedIds by remember { mutableStateOf(setOf<String>()) }
            val allIds = mockTable.rows.map { it.palletId }
            val allSelected = selectedIds.size == allIds.size && allIds.isNotEmpty()
            Column(Modifier.fillMaxSize().background(tc.screenBg)) {
                Row(
                    modifier = Modifier.fillMaxWidth().background(tc.headerBg)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("📦  ${mockTable.rows.size} rows  •  ${TABLE_COLUMNS.size} columns",
                        color = tc.headerTxt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Pinch to zoom", color = tc.headerTxt.copy(alpha = 0.45f), fontSize = 11.sp)
                }
                TableGrid(
                    tableData = mockTable,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    allSelected = allSelected,
                    tc = tc,
                    onToggleSelectAll = {
                        selectedIds = if (allSelected) emptySet() else allIds.toSet()
                    },
                    onToggleRow = { id ->
                        selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                    },
                    onCopyRow = {},
                    onCopyCell = {}
                )
            }
        }
    }
}
