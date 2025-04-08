package com.jetpackduba.gitnuro.extensions

import androidx.compose.runtime.Composable
import com.jetpackduba.gitnuro.LocalDateTimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Composable
fun Instant.toSmartSystemString(
    allowRelative: Boolean = true,
    useSystemDefaultFormat: Boolean? = null,
    showTime: Boolean = false,
): String {
    val dateTimeFormat = LocalDateTimeFormat.current
    val useSystemDefault = useSystemDefaultFormat ?: dateTimeFormat.useSystemDefault

    val zoneId = ZoneId.systemDefault()
    val localDate = atZone(zoneId).toLocalDate()
    val currentTime = LocalDate.now(zoneId)

    val formattedDate = if (
        dateTimeFormat.useRelativeDate &&
        allowRelative &&
        localDate.isTodayOrYesterday(currentTime)
    ) {
        if (localDate.dayOfMonth == currentTime.dayOfMonth)
            "Today"
        else
            "Yesterday"
    } else {
        val systemLocale = System.getProperty("user.language")
        val locale = Locale(systemLocale)

        val formatter = if (useSystemDefault) {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        } else {
            DateTimeFormatter.ofPattern(dateTimeFormat.customFormat, locale)
        }

        formatter.format(localDate)
    }

    val formattedTime = if (showTime) {
        val localDateTime = atZone(zoneId).toLocalDateTime()

        val timeFormatter = if (useSystemDefault) {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        } else if (dateTimeFormat.is24hours) {
            DateTimeFormatter.ofPattern("HH:mm")
        } else {
            DateTimeFormatter.ofPattern("hh:mm a")
        }

        timeFormatter.format(localDateTime)
    } else {
        ""
    }

    return "${formattedDate.trim()} ${formattedTime.trim()}".trim()
}

private fun LocalDate.isTodayOrYesterday(
    currentTime: LocalDate,
): Boolean {
    return this.year == currentTime.year &&
            this.month == currentTime.month &&
            this.dayOfMonth in (currentTime.dayOfMonth - 1)..currentTime.dayOfMonth
}