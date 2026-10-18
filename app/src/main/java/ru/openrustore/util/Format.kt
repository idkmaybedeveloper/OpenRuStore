package ru.openrustore.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * Android releases do not map onto SDK levels by a single rule: everything up to Honeycomb has its
 * own name, 28..31 sit one below "API minus nineteen" and 33 and later shift by twenty, while 32
 * is the odd 12L. The table covers the irregular part and the arithmetic covers the rest.
 * ref: https://en.wikipedia.org/wiki/Android_version_history
 */
private val sdkExceptions =
  mapOf(
    1 to "1.0",
    2 to "1.1",
    3 to "1.5",
    4 to "1.6",
    5 to "2.0",
    6 to "2.0.1",
    7 to "2.1",
    8 to "2.2",
    9 to "2.3",
    10 to "2.3.3",
    11 to "3.0",
    12 to "3.1",
    13 to "3.2",
    14 to "4.0",
    15 to "4.0.3",
    16 to "4.1",
    17 to "4.2",
    18 to "4.3",
    19 to "4.4",
    20 to "4.4W",
    21 to "5.0",
    22 to "5.1",
    23 to "6.0",
    24 to "7.0",
    25 to "7.1",
    26 to "8.0",
    27 to "8.1",
    32 to "12L",
  )

fun androidVersionOf(sdk: Int): String =
  when {
    sdkExceptions.containsKey(sdk) -> "Android ${sdkExceptions[sdk]}"
    sdk in 28..31 -> "Android ${sdk - 19}"
    sdk >= 33 -> "Android ${sdk - 20}"
    else -> "API $sdk"
  }

fun formatSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val units = listOf("B", "KB", "MB", "GB")
  val power = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
  val value = bytes / 1024.0.pow(power)
  return if (power == 0) "${value.toInt()} ${units[power]}" else String.format(Locale.US, "%.1f %s", value, units[power])
}

fun formatCount(value: Long): String = String.format(Locale.US, "%,d", value)

/**
 * RuStore mixes two shapes of timestamps: `2026-10-16T15:00:27.086+00:00` for application data and
 * `2026-10-18 15:32:50.273` for comments. Only the day is ever shown, so the date part is enough
 * and parsing it avoids pulling in core library desugaring for java.time on API 24.
 */
fun formatDate(raw: String?): String {
  if (raw.isNullOrBlank()) return "-"
  val day = raw.take(10)
  val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day) }.getOrNull() ?: return day
  return DateFormat.getDateInstance(DateFormat.MEDIUM).format(parsed)
}

fun formatRating(value: Double): String = String.format(Locale.US, "%.2f", value)

/** Descriptions and release notes arrive with escapes left over from the web API. */
fun unescapeText(raw: String): String =
  raw.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\"", "\"").trim()
