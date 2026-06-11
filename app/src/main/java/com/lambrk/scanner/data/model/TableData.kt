package com.lambrk.scanner.data.model

data class TableRow(
    val palletNo: String,
    val binId: String,
    val productCode: String,
    val qty: String,
    val date: String,
    val hReason: String,
    val grade: String
)

data class TableData(
    val rows: List<TableRow>
)
