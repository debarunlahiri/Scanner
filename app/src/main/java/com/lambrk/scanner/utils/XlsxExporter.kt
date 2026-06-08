package com.lambrk.scanner.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.lambrk.scanner.data.model.TableData
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Zero-dependency XLSX writer.
 * XLSX is a ZIP archive of XML files — we build it directly with ZipOutputStream.
 * Compatible with minSdk 24+.
 */
object XlsxExporter {

    /**
     * Writes [tableData] to an .xlsx file in the app's cache directory,
     * then returns a shareable [Uri] via FileProvider.
     */
    fun export(context: Context, tableData: TableData, fileName: String = "export.xlsx"): Uri {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { fos ->
            ZipOutputStream(fos).use { zip ->
                writeContentTypes(zip)
                writeRels(zip)
                writeWorkbook(zip)
                writeWorkbookRels(zip)
                writeStyles(zip)
                writeSheet(zip, tableData)
            }
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Opens an Intent chooser so the user can share / open the XLSX file.
     */
    fun shareExcel(context: Context, uri: Uri, fileName: String = "export.xlsx") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Download / Share Excel"))
    }

    // ── ZIP entries ────────────────────────────────────────────────────────────

    private fun writeContentTypes(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml"  ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml"
            ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml"
            ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml"
            ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent().toByteArray()
        )
        zip.closeEntry()
    }

    private fun writeRels(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"
                Target="xl/workbook.xml"/>
</Relationships>""".trimIndent().toByteArray()
        )
        zip.closeEntry()
    }

    private fun writeWorkbook(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        zip.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Result" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent().toByteArray()
        )
        zip.closeEntry()
    }

    private fun writeWorkbookRels(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zip.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles"
                Target="styles.xml"/>
</Relationships>""".trimIndent().toByteArray()
        )
        zip.closeEntry()
    }

    private fun writeStyles(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        zip.write(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>""".trimIndent().toByteArray()
        )
        zip.closeEntry()
    }

    private fun writeSheet(zip: ZipOutputStream, tableData: TableData) {
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Column widths — set generous width for all columns
        sb.append("<cols>")
        tableData.columns.forEachIndexed { idx, _ ->
            sb.append("""<col min="${idx + 1}" max="${idx + 1}" width="22" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Header row (row 1, bold style index = 1)
        sb.append("""<row r="1">""")
        tableData.columns.forEachIndexed { colIdx, col ->
            val cellRef = cellAddress(0, colIdx)
            sb.append("""<c r="$cellRef" t="inlineStr" s="1"><is><t>${col.header.xmlEscape()}</t></is></c>""")
        }
        sb.append("</row>")

        // Data rows
        tableData.rows.forEachIndexed { rowIdx, row ->
            val excelRow = rowIdx + 2 // 1-indexed, row 1 is header
            sb.append("""<row r="$excelRow">""")
            tableData.columns.forEachIndexed { colIdx, col ->
                val cellRef = cellAddress(rowIdx + 1, colIdx)
                val value = (row.cells[col.key] ?: "").xmlEscape()
                sb.append("""<c r="$cellRef" t="inlineStr"><is><t>$value</t></is></c>""")
            }
            sb.append("</row>")
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        zip.write(sb.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /** Converts (rowIndex, colIndex) to Excel cell address, e.g. (0,0) → "A1" */
    private fun cellAddress(rowIndex: Int, colIndex: Int): String {
        val col = buildString {
            var n = colIndex
            do {
                insert(0, ('A' + (n % 26)))
                n = n / 26 - 1
            } while (n >= 0)
        }
        return "$col${rowIndex + 1}"
    }

    private fun String.xmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
