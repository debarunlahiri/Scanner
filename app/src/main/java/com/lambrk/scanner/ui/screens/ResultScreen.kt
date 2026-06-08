package com.lambrk.scanner.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lambrk.scanner.data.model.TableColumn
import com.lambrk.scanner.data.model.TableData
import com.lambrk.scanner.data.model.TableRow
import com.lambrk.scanner.ui.components.ConfigureSystemBars
import com.lambrk.scanner.ui.navigation.Screen
import com.lambrk.scanner.ui.viewmodel.ResultUiState
import com.lambrk.scanner.ui.viewmodel.ResultViewModel
import com.lambrk.scanner.utils.XlsxExporter

// ─── Dimensions ────────────────────────────────────────────────────────────────
private val CELL_W: Dp    = 160.dp
private val CELL_H: Dp    = 52.dp
private val HDR_H: Dp     = 50.dp
private val IDX_W: Dp     = 46.dp
private val CHK_W: Dp     = 46.dp
private val ACT_W: Dp     = 70.dp

// ─── Colours ───────────────────────────────────────────────────────────────────
private val ColHeaderBg   = Color(0xFF1A2744)
private val ColHeaderTxt  = Color(0xFFE8F0FE)
private val RowEven       = Color(0xFFF5F8FF)
private val RowOdd        = Color(0xFFEBEFF9)
private val RowSelected   = Color(0xFFD0E4FF)
private val CellBorder    = Color(0xFFCDD5E0)
private val PalletBlue    = Color(0xFF1565C0)
private val AccentOrange  = Color(0xFFFF8000)
private val IndexGrey     = Color(0xFF78909C)
private val SelectionBar  = Color(0xFF0D47A1)

// ─── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(navController: NavController, qrData: String) {
    ConfigureSystemBars(statusBarColor = MaterialTheme.colorScheme.primary, lightIcons = true)

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
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = Color(0xFFEDF1FB)
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
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 4.dp)
        Spacer(Modifier.height(16.dp))
        Text("Loading table…", style = MaterialTheme.typography.titleMedium)
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

    // Multi-select state: set of selected pallet IDs
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val allIds = state.tableData.rows.map { it.palletId }
    val allSelected = selectedIds.size == allIds.size && allIds.isNotEmpty()

    Column(Modifier.fillMaxSize()) {
        // ── Summary banner ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColHeaderBg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "📦  ${state.tableData.rows.size} rows  •  ${state.tableData.columns.size} columns",
                color = ColHeaderTxt,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text("Pinch to zoom", color = ColHeaderTxt.copy(alpha = 0.55f), fontSize = 11.sp)
        }

        // ── Selection action bar (visible when ≥1 row selected) ─────────────
        AnimatedVisibility(
            visible = selectedIds.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            SelectionBar(
                selectedCount = selectedIds.size,
                totalCount = allIds.size,
                allSelected = allSelected,
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
                    selectedIds = selectedIds,
                    allSelected = allSelected,
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
    onSelectAll: () -> Unit,
    onCopySelected: () -> Unit,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SelectionBar)
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
                    containerColor = AccentOrange,
                    contentColor = Color.White
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
    selectedIds: Set<String>,
    allSelected: Boolean,
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
                .background(ColHeaderBg)
        ) {
            // Select-all checkbox header
            CheckboxHeaderCell(allSelected = allSelected, onToggle = onToggleSelectAll)
            // Row index header
            HeaderCell(text = "#", width = IDX_W)
            // Column headers
            tableData.columns.forEach { col -> HeaderCell(text = col.header, width = CELL_W) }
            // Action header
            HeaderCell(text = "Copy ID", width = ACT_W)
        }

        // ── Data rows ──
        LazyColumn {
            itemsIndexed(tableData.rows) { idx, row ->
                DataRow(
                    index = idx + 1,
                    row = row,
                    columns = tableData.columns,
                    isEven = idx % 2 == 0,
                    isSelected = selectedIds.contains(row.palletId),
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
private fun CheckboxHeaderCell(allSelected: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .width(CHK_W)
            .height(HDR_H)
            .border(0.5.dp, ColHeaderTxt.copy(alpha = 0.2f))
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (allSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
            contentDescription = "Select All",
            tint = if (allSelected) AccentOrange else ColHeaderTxt.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(HDR_H)
            .border(0.5.dp, ColHeaderTxt.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ColHeaderTxt,
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
    columns: List<TableColumn>,
    isEven: Boolean,
    isSelected: Boolean,
    hScroll: ScrollState,
    onToggleSelect: () -> Unit,
    onCopyRow: (String) -> Unit,
    onCopyCell: (String) -> Unit
) {
    val bg = when {
        isSelected -> RowSelected
        isEven     -> RowEven
        else       -> RowOdd
    }

    Row(
        modifier = Modifier
            .horizontalScroll(hScroll)
            .background(bg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Checkbox cell ──
        Box(
            modifier = Modifier
                .width(CHK_W)
                .height(CELL_H)
                .border(0.5.dp, CellBorder)
                .clickable { onToggleSelect() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                contentDescription = "Select row",
                tint = if (isSelected) PalletBlue else Color(0xFFB0BEC5),
                modifier = Modifier.size(22.dp)
            )
        }

        // ── Row index ──
        Box(
            modifier = Modifier
                .width(IDX_W)
                .height(CELL_H)
                .border(0.5.dp, CellBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$index",
                fontSize = 11.sp,
                color = IndexGrey,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Data cells ──
        columns.forEach { col ->
            val value = row.cells[col.key] ?: ""
            Box(
                modifier = Modifier
                    .width(CELL_W)
                    .height(CELL_H)
                    .border(0.5.dp, CellBorder),
                contentAlignment = Alignment.CenterStart
            ) {
                if (col.key == "palletId") {
                    PalletChip(
                        palletId = value,
                        isSelected = isSelected,
                        onCopy = { onCopyCell(value) }
                    )
                } else {
                    Text(
                        text = value,
                        fontSize = 12.sp,
                        color = Color(0xFF1A1A2E),
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
                .border(0.5.dp, CellBorder),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { onCopyRow(row.palletId) },
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy Pallet ID",
                    tint = AccentOrange,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── Pallet ID chip ────────────────────────────────────────────────────────────

@Composable
private fun PalletChip(palletId: String, isSelected: Boolean, onCopy: () -> Unit) {
    val chipColor = if (isSelected) PalletBlue else PalletBlue.copy(alpha = 0.85f)

    Row(
        modifier = Modifier
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(chipColor.copy(alpha = if (isSelected) 0.18f else 0.1f))
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = palletId,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = chipColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 92.dp)
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(chipColor),
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

// ─── Clipboard ─────────────────────────────────────────────────────────────────

private fun copyText(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Pallet", text))
    val display = if (text.length > 60) text.take(60) + "…" else text
    Toast.makeText(context, "Copied: $display", Toast.LENGTH_SHORT).show()
}
