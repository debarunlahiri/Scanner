package com.lambrk.scanner.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambrk.scanner.data.model.Post
import com.lambrk.scanner.data.model.TableData
import com.lambrk.scanner.data.model.TableRow
import com.lambrk.scanner.data.network.RetrofitClient
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

sealed class ResultUiState {
    data object Loading : ResultUiState()
    data class Success(
        val post: Post,
        val qrCode: String,
        val tableData: TableData
    ) : ResultUiState()
    data class Error(val message: String, val qrCode: String) : ResultUiState()
}

class ResultViewModel(private val qrCode: String) : ViewModel() {

    private val _uiState = mutableStateOf<ResultUiState>(ResultUiState.Loading)
    val uiState: State<ResultUiState> = _uiState

    init { fetchData() }

    fun retry() { fetchData() }

    private fun fetchData() {
        _uiState.value = ResultUiState.Loading
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getPost(qrCode.toValidId())
                _uiState.value = ResultUiState.Success(
                    post = response,
                    qrCode = qrCode,
                    tableData = buildTableData()
                )
            } catch (e: Exception) {
                _uiState.value = ResultUiState.Error(e.message ?: "Something went wrong", qrCode)
            }
        }
    }

    private fun String.toValidId() = hashCode().absoluteValue % 100 + 1

    private fun buildTableData() = TableData(
        rows = listOf(
            TableRow("PLT-0001", "SKU-1001", "Widget A",      "12",  "1.50", "A-01-01", "Received",   "2025-06-01 09:00", "U-1"),
            TableRow("PLT-0002", "SKU-1002", "Widget B",       "5",  "3.20", "B-02-03", "In Transit", "2025-06-02 10:15", "U-2"),
            TableRow("PLT-0003", "SKU-1003", "Gadget Pro",    "30",  "0.80", "C-03-02", "Stored",     "2025-06-03 11:30", "U-1"),
            TableRow("PLT-0004", "SKU-1004", "Heavy Unit",     "2", "12.00", "D-04-01", "Dispatched", "2025-06-04 08:45", "U-3"),
            TableRow("PLT-0005", "SKU-1005", "Small Part",   "100",  "0.25", "E-05-05", "Pending",    "2025-06-05 14:00", "U-2"),
            TableRow("PLT-0006", "SKU-1006", "Component X",   "50",  "2.10", "F-06-04", "Stored",     "2025-06-06 09:30", "U-1"),
            TableRow("PLT-0007", "SKU-1007", "Assembly Kit",   "8",  "5.60", "A-01-01", "Received",   "2025-06-07 13:20", "U-4"),
            TableRow("PLT-0008", "SKU-1008", "Bolt Pack",    "200",  "0.10", "B-02-03", "Stored",     "2025-06-08 07:50", "U-2"),
            TableRow("PLT-0009", "SKU-1009", "Motor Unit",     "3", "18.40", "C-03-02", "In Transit", "2025-06-09 16:00", "U-3"),
            TableRow("PLT-0010", "SKU-1010", "Sensor Array",  "15",  "1.90", "D-04-01", "Pending",    "2025-06-10 11:10", "U-1")
        )
    )
}
