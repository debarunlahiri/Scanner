package com.lambrk.scanner.utils

fun parseQrContent(raw: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    when {
        raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true) -> {
            result.add("Type" to "URL")
            result.add("Link" to raw)
        }
        raw.startsWith("WIFI:", ignoreCase = true) -> {
            result.add("Type" to "Wi-Fi")
            val ssid = Regex("""S:([^;]+)""").find(raw)?.groupValues?.get(1) ?: "N/A"
            val security = Regex("""T:([^;]+)""").find(raw)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: "WPA/WPA2"
            val password = Regex("""P:([^;]+)""").find(raw)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: "None"
            result.add("Network" to ssid)
            result.add("Security" to security)
            result.add("Password" to password)
        }
        raw.startsWith("mailto:", ignoreCase = true) -> {
            result.add("Type" to "Email")
            result.add("Address" to raw.removePrefix("mailto:"))
        }
        raw.startsWith("tel:", ignoreCase = true) -> {
            result.add("Type" to "Phone")
            result.add("Number" to raw.removePrefix("tel:"))
        }
        raw.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
            result.add("Type" to "Contact")
            val name = Regex("""FN[:;]([^\r\n]+)""").find(raw)?.groupValues?.get(1)?.trim() ?: "N/A"
            val phone = Regex("""TEL[:;]([^\r\n]+)""").find(raw)?.groupValues?.get(1)?.trim() ?: "N/A"
            result.add("Name" to name)
            result.add("Phone" to phone)
        }
        raw.startsWith("BEGIN:VEVENT", ignoreCase = true) -> {
            result.add("Type" to "Event")
            val summary = Regex("""SUMMARY[:;]([^\r\n]+)""").find(raw)?.groupValues?.get(1)?.trim() ?: "N/A"
            result.add("Summary" to summary)
        }
        else -> {
            result.add("Type" to "Plain Text")
            result.add("Content" to raw)
        }
    }
    result.add("Length" to "${raw.length} chars")
    return result
}
