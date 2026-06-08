package com.lambrk.scanner.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambrk.scanner.data.model.Post
import com.lambrk.scanner.data.model.TableData
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

    init {
        fetchData()
    }

    fun retry() {
        fetchData()
    }

    private fun fetchData() {
        _uiState.value = ResultUiState.Loading
        viewModelScope.launch {
            try {
                val id = qrCode.toValidId()
                val response = RetrofitClient.apiService.getPost(id)
                _uiState.value = ResultUiState.Success(
                    post = response,
                    qrCode = qrCode,
                    tableData = TableData.fromQrCode(qrCode, response)
                )
            } catch (e: Exception) {
                _uiState.value = ResultUiState.Error(
                    e.message ?: "Something went wrong",
                    qrCode
                )
            }
        }
    }

    private fun String.toValidId(): Int {
        return this.hashCode().absoluteValue % 100 + 1
    }
}
