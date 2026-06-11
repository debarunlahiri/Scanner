package com.lambrk.scanner.utils

import android.content.Context
import android.net.Uri

data class GeneratedExcelFile(
    val name: String,
    val uri: Uri,
    val createdAt: Long
)

object GeneratedExcelRegistry {
    private const val PREFS = "generated_excel_files"
    private const val KEY_FILES = "files"
    private const val SEPARATOR = "|||"

    fun add(context: Context, name: String, uri: Uri) {
        val files = list(context).toMutableList()
        files.removeAll { it.uri == uri }
        files.add(0, GeneratedExcelFile(name = name, uri = uri, createdAt = System.currentTimeMillis()))

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_FILES, files.take(25).map { it.serialize() }.toSet())
            .apply()
    }

    fun list(context: Context): List<GeneratedExcelFile> {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_FILES, emptySet())
            .orEmpty()
            .mapNotNull { it.deserialize() }
            .sortedByDescending { it.createdAt }
    }

    private fun GeneratedExcelFile.serialize(): String {
        return listOf(name, uri.toString(), createdAt.toString()).joinToString(SEPARATOR)
    }

    private fun String.deserialize(): GeneratedExcelFile? {
        val parts = split(SEPARATOR)
        if (parts.size != 3) return null
        return GeneratedExcelFile(
            name = parts[0],
            uri = Uri.parse(parts[1]),
            createdAt = parts[2].toLongOrNull() ?: return null
        )
    }
}
