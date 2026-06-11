package com.lambrk.scanner.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambrk.scanner.data.model.PalletTableBuilder
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
                    tableData = PalletTableBuilder.build(
                        binId = qrCode,
                        responseRows = response.toPalletRows(qrCode)
                    )
                )
            } catch (e: Exception) {
                _uiState.value = ResultUiState.Error(e.message ?: "Something went wrong", qrCode)
            }
        }
    }

    private fun String.toValidId() = hashCode().absoluteValue % 100 + 1

    private fun Post.toPalletRows(binId: String): List<TableRow> {
        val palletNos = PalletTableBuilder.demoPalletNosForBin(binId)
        return palletNos.take(4).mapIndexed { index, palletNo ->
            TableRow(
                palletNo = palletNo,
                binId = binId,
                productCode = "PC-${id.toString().padStart(4, '0')}-${index + 1}",
                qty = ((index + 1) * 5).toString(),
                date = "",
                hReason = body.lineSequence().firstOrNull().orEmpty(),
                grade = if (index % 2 == 0) "A1" else "S1"
            )
        }
    }
}
