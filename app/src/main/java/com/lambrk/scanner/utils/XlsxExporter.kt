package com.lambrk.scanner.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.lambrk.scanner.data.model.TableData
import com.lambrk.scanner.data.model.TableRow
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
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
        FileOutputStream(file).use { output -> writeWorkbookFile(output, tableData) }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Saves the XLSX locally without requiring internet, Play services, or another app.
     */
    fun saveToDownloads(context: Context, tableData: TableData, fileName: String = "export.xlsx"): Uri {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToPublicDownloads(context, tableData, fileName)
        } else {
            saveToAppDownloads(context, tableData, fileName)
        }
        GeneratedExcelRegistry.add(context, fileName, uri)
        return uri
    }

    fun read(context: Context, uri: Uri): TableData {
        val entries = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/worksheets/sheet1.xml" || entry.name == "xl/sharedStrings.xml") {
                        entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    entry = zip.nextEntry
                }
            }
        }

        val sheetXml = entries["xl/worksheets/sheet1.xml"] ?: return TableData(emptyList())
        val sharedStrings = entries["xl/sharedStrings.xml"]?.let(::parseSharedStrings).orEmpty()

        val rows = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
            .findAll(sheetXml)
            .drop(1)
            .map { rowMatch ->
                Regex("<c([^>]*)>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(rowMatch.groupValues[1])
                    .map { cellMatch ->
                        val attributes = cellMatch.groupValues[1]
                        val content = cellMatch.groupValues[2]
                        val inlineValue = Regex("<t>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                            .find(content)
                            ?.groupValues
                            ?.get(1)
                            ?.xmlUnescape()
                        val rawValue = Regex("<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
                            .find(content)
                            ?.groupValues
                            ?.get(1)
                            ?.xmlUnescape()

                        if (attributes.contains("""t="s"""")) {
                            sharedStrings.getOrElse(rawValue?.toIntOrNull() ?: -1) { "" }
                        } else {
                            inlineValue ?: rawValue.orEmpty()
                        }
                    }
                    .toList()
            }
            .map { cells ->
                TableRow(
                    palletNo = cells.getOrElse(0) { "" },
                    binId = cells.getOrElse(1) { "" },
                    productCode = cells.getOrElse(2) { "" },
                    qty = cells.getOrElse(3) { "" },
                    date = cells.getOrElse(4) { "" },
                    hReason = cells.getOrElse(5) { "" },
                    grade = cells.getOrElse(6) { "" }
                )
            }
            .toList()

        return TableData(rows)
    }

    private fun parseSharedStrings(xml: String): List<String> {
        return Regex("<si[^>]*>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .map { siMatch ->
                Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(siMatch.groupValues[1])
                    .joinToString("") { it.groupValues[1].xmlUnescape() }
            }
            .toList()
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

    private fun saveToPublicDownloads(context: Context, tableData: TableData, fileName: String): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, XLSX_MIME_TYPE)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create Downloads file")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                writeWorkbookFile(output, tableData)
            } ?: error("Could not open Downloads file")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun saveToAppDownloads(context: Context, tableData: TableData, fileName: String): Uri {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, fileName)
        FileOutputStream(file).use { output -> writeWorkbookFile(output, tableData) }

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun writeWorkbookFile(output: OutputStream, tableData: TableData) {
        ZipOutputStream(output).use { zip ->
            writeContentTypes(zip)
            writeRels(zip)
            writeWorkbook(zip)
            writeWorkbookRels(zip)
            writeStyles(zip)
            writeSheet(zip, tableData)
        }
    }

    // ── ZIP entries ────────────────────────────────────────────────────────────

    private const val XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

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

    private val HEADERS = listOf(
        "Pallet No"    to { r: com.lambrk.scanner.data.model.TableRow -> r.palletNo },
        "Bin ID"       to { r: com.lambrk.scanner.data.model.TableRow -> r.binId },
        "Product Code" to { r: com.lambrk.scanner.data.model.TableRow -> r.productCode },
        "Qty"          to { r: com.lambrk.scanner.data.model.TableRow -> r.qty },
        "Date"         to { r: com.lambrk.scanner.data.model.TableRow -> r.date },
        "H Reason"     to { r: com.lambrk.scanner.data.model.TableRow -> r.hReason },
        "Grade"        to { r: com.lambrk.scanner.data.model.TableRow -> r.grade }
    )

    private fun writeSheet(zip: ZipOutputStream, tableData: TableData) {
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Column widths
        sb.append("<cols>")
        HEADERS.forEachIndexed { idx, _ ->
            sb.append("""<col min="${idx + 1}" max="${idx + 1}" width="22" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Header row (bold style = 1)
        sb.append("""<row r="1">""")
        HEADERS.forEachIndexed { colIdx, (header, _) ->
            val cellRef = cellAddress(0, colIdx)
            sb.append("""<c r="$cellRef" t="inlineStr" s="1"><is><t>${header.xmlEscape()}</t></is></c>""")
        }
        sb.append("</row>")

        // Data rows
        tableData.rows.forEachIndexed { rowIdx, row ->
            val excelRow = rowIdx + 2
            sb.append("""<row r="$excelRow">""")
            HEADERS.forEachIndexed { colIdx, (_, accessor) ->
                val cellRef = cellAddress(rowIdx + 1, colIdx)
                val value = accessor(row).xmlEscape()
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

    private fun String.xmlUnescape(): String = this
        .replace("&apos;", "'")
        .replace("&quot;", "\"")
        .replace("&gt;", ">")
        .replace("&lt;", "<")
        .replace("&amp;", "&")
}
