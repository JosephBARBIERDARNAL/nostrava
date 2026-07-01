package com.didit.app.util

import java.util.Calendar

/** Shifts a history-range anchor timestamp by one week/month/year, matching ActivityHistory.tsx's shift(). */
fun shiftAnchor(anchorMs: Long, range: String, forward: Boolean): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = anchorMs
    val delta = if (forward) 1 else -1
    when (range) {
        "week" -> cal.add(Calendar.DAY_OF_MONTH, 7 * delta)
        "month" -> cal.add(Calendar.MONTH, delta)
        "year" -> cal.add(Calendar.YEAR, delta)
    }
    return cal.timeInMillis
}
