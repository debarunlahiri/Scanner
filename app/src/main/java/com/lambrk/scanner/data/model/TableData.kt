package com.lambrk.scanner.data.model

data class TableRow(
    val palletId: String,
    val sku: String,
    val description: String,
    val qty: String,
    val weight: String,
    val location: String,
    val status: String,
    val scanTime: String,
    val userId: String
)

data class TableData(
    val rows: List<TableRow>
)
