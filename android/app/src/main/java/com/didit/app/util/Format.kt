package com.didit.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

fun formatDuration(ms: Long): String {
    val safe = if (ms < 0) 0 else ms
    val total = safe / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

fun formatDistance(m: Double?): String {
    val v = m ?: 0.0
    return if (v < 1000) String.format(Locale.US, "%.0f m", v)
    else String.format(Locale.US, "%.2f km", v / 1000)
}

fun formatPace(secPerKm: Double?): String {
    val v = secPerKm ?: 0.0
    if (!v.isFinite() || v <= 0) return "—"
    val m = floor(v / 60).toInt()
    val s = (v % 60).roundToInt()
    return String.format(Locale.US, "%d:%02d/km", m, s)
}

fun formatSpeed(secPerKm: Double?): String {
    val v = secPerKm ?: 0.0
    if (!v.isFinite() || v <= 0) return "—"
    val kmh = 3600 / v
    return String.format(Locale.US, "%.1f km/h", kmh)
}

fun formatDate(ms: Long): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))

fun formatDateTime(ms: Long): String =
    SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(ms))

fun formatInstallDate(ms: Long): String {
    val date = SimpleDateFormat("d MMMM", Locale.UK).format(Date(ms))
    val time = SimpleDateFormat("HH:mm", Locale.UK).format(Date(ms))
    return "$date, $time"
}
