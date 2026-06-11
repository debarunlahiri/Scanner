package com.lambrk.scanner.data.model

private const val DEFAULT_DEMO_PALLETS_PER_BIN = 14

object PalletTableBuilder {
    private val dynamicBinIds = setOf("G-RW", "PACK-ZONE")

    private val fixedPalletCountByBinId = mapOf(
        "G06B-03" to 7,
        "G06A-03" to 14,
        "G06B-02" to 4,
        "G06A-02" to 12,
        "G06B-01" to 6,
        "G06A-01" to 10,
        "GVR05" to 8,
        "G05B-03" to 5,
        "G05A-03" to 13,
        "G05B-02" to 4,
        "G05A-02" to 11,
        "G05B-01" to 6,
        "G05A-01" to 9,
        "GVR04" to 8,
        "G04B-03" to 7,
        "G04A-03" to 14,
        "G04B-02" to 4,
        "G04A-02" to 12,
        "G04B-01" to 6,
        "G04A-01" to 10,
        "GVR03" to 8,
        "G03B-03" to 5,
        "G03A-03" to 13,
        "G03B-02" to 4,
        "G03A-02" to 11,
        "G03B-01" to 6,
        "G03A-01" to 9,
        "GVR02" to 8,
        "G02B-03" to 7,
        "G02A-03" to 14,
        "G02B-02" to 4,
        "G02A-02" to 12,
        "G02B-01" to 6,
        "G02A-01" to 10,
        "GVR01" to 8,
        "G01X-03" to 7,
        "G01X-02" to 4,
        "G01X-01" to 6,
        "G01X-04" to 9,
        "GVR06" to 8,
        "GVR07" to 8
    )

    private val fixedPalletNosByBinId = fixedPalletCountByBinId.mapValues { entry ->
        (1..entry.value).map { palletIndex -> palletNoForBin(entry.key, palletIndex) }
    }

    fun build(binId: String, responseRows: List<TableRow>): TableData {
        val normalizedBinId = normalizeBinId(binId)
        if (normalizedBinId in dynamicBinIds) {
            return TableData(rows = responseRows)
        }

        val palletCount = fixedPalletCountByBinId[normalizedBinId].orEmptyCount()
        val availableRows = responseRows.take(palletCount)
        val emptyRows = List((palletCount - availableRows.size).coerceAtLeast(0)) {
            emptyRow()
        }
        val rows = availableRows + emptyRows

        return TableData(rows = rows)
    }

    fun fixedPalletNosForBin(binId: String): List<String> {
        return fixedPalletNosByBinId[normalizeBinId(binId)].orEmpty()
    }

    fun demoPalletNosForBin(binId: String): List<String> {
        val fixedPalletNos = fixedPalletNosForBin(binId)
        if (fixedPalletNos.isNotEmpty()) return fixedPalletNos

        return (1..DEFAULT_DEMO_PALLETS_PER_BIN).map { index ->
            palletNoForBin(binId, index)
        }
    }

    private fun emptyRow() = TableRow(
        palletNo = "",
        binId = "",
        productCode = "",
        qty = "",
        date = "",
        hReason = "",
        grade = ""
    )

    private fun normalizeBinId(binId: String) = binId.trim().uppercase()

    private fun palletNoForBin(binId: String, palletIndex: Int): String {
        val prefix = if (palletIndex % 2 == 0) "M" else "Z"
        val seed = "${normalizeBinId(binId)}-$palletIndex".hashCode().toLong() and 0xFFFFFFFFL
        return prefix + java.lang.Long.toHexString(seed).uppercase().padStart(8, '0').take(8)
    }

    private fun Int?.orEmptyCount() = this ?: 0
}
