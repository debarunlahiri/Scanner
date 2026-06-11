package com.lambrk.scanner.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PalletTableBuilderTest {
    @Test
    fun build_returnsConfiguredRowsWithResponseRowsMergedByPalletNo() {
        val responseRows = listOf(
            TableRow("Z001A2B3C", "G06B-03", "PC-1", "2", "2026-06-11", "Hold", "A1"),
            TableRow("M00AF12C3", "G06B-03", "PC-4", "8", "2026-06-11", "", "S1")
        )

        val tableData = PalletTableBuilder.build("G06B-03", responseRows)

        assertEquals(7, tableData.rows.size)
        assertEquals("PC-1", tableData.rows[0].productCode)
        assertEquals("PC-4", tableData.rows[1].productCode)
        assertEquals("Z001A2B3C", tableData.rows[0].palletNo)
        assertEquals("M00AF12C3", tableData.rows[1].palletNo)
        assertTrue(tableData.rows[2].palletNo.isEmpty())
        assertTrue(tableData.rows[2].binId.isEmpty())
        assertTrue(tableData.rows[6].productCode.isEmpty())
    }

    @Test
    fun build_keepsDynamicBinsWithoutAddingEmptyFixedRows() {
        val responseRows = listOf(
            TableRow("Z001A2B3C", "PACK-ZONE", "PC-1", "2", "2026-06-11", "Hold", "A1")
        )

        val tableData = PalletTableBuilder.build("PACK-ZONE", responseRows)

        assertEquals(responseRows, tableData.rows)
    }
}
