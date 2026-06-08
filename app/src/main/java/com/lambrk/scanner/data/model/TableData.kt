package com.lambrk.scanner.data.model

/**
 * Represents a single column definition in the result table.
 */
data class TableColumn(
    val key: String,
    val header: String
)

/**
 * Represents a single row in the result table.
 * [palletId] is the primary identifier that can be copied.
 * [cells] maps column keys to their cell values.
 */
data class TableRow(
    val palletId: String,
    val cells: Map<String, String>
)

/**
 * Complete table data for the result screen.
 * Maximum 9 columns supported.
 */
data class TableData(
    val columns: List<TableColumn>,
    val rows: List<TableRow>
) {
    companion object {
        const val MAX_COLUMNS = 9

        /**
         * Creates a sample/mock TableData from a QR code string.
         * In production, this would be replaced by a real API response parser.
         */
        fun fromQrCode(qrCode: String, post: Post): TableData {
            val columns = listOf(
                TableColumn("palletId", "Pallet ID"),
                TableColumn("sku", "SKU"),
                TableColumn("description", "Description"),
                TableColumn("qty", "Qty"),
                TableColumn("weight", "Weight (kg)"),
                TableColumn("location", "Location"),
                TableColumn("status", "Status"),
                TableColumn("scanTime", "Scan Time"),
                TableColumn("userId", "User ID")
            )

            // Generate rich sample rows using the QR code and post data
            val rows = buildSampleRows(qrCode, post)

            return TableData(columns = columns, rows = rows)
        }

        private fun buildSampleRows(qrCode: String, post: Post): List<TableRow> {
            val hash = qrCode.hashCode().let { if (it < 0) -it else it }
            val statuses = listOf("Received", "In Transit", "Stored", "Dispatched", "Pending", "Damaged")
            val locations = listOf("A-01-01", "B-02-03", "C-03-02", "D-04-01", "E-05-05", "F-06-04")
            val skuPrefix = listOf("SKU-", "ITEM-", "PROD-", "GOOD-")

            return (1..20).map { index ->
                val palletId = "PLT-${"%04d".format((hash + index * 37) % 9999)}"
                TableRow(
                    palletId = palletId,
                    cells = mapOf(
                        "palletId" to palletId,
                        "sku" to "${skuPrefix[index % skuPrefix.size]}${(hash + index * 13) % 100000}",
                        "description" to "Item ${post.title.take(20).trim()}…",
                        "qty" to "${(hash + index * 7) % 500 + 1}",
                        "weight" to "${"%.2f".format(((hash + index * 3) % 500 + 1) / 10.0)}",
                        "location" to locations[(index + hash) % locations.size],
                        "status" to statuses[(index + hash) % statuses.size],
                        "scanTime" to "2025-06-${"%02d".format(index % 30 + 1)} 0${index % 9}:${"%02d".format(index * 3 % 60)}",
                        "userId" to "U-${post.userId}-${(hash + index) % 99}"
                    )
                )
            }
        }
    }
}
