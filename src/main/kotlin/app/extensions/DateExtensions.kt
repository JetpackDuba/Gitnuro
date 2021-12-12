package app.extensions

import java.text.DateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

fun Date.toSmartSystemString(): String {
    val systemLocale = System.getProperty("user.language")
    val locale = Locale(systemLocale)
    val sdf = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

    val zoneId = ZoneId.systemDefault()
    val localDate = this.toInstant().atZone(zoneId).toLocalDate();
    val currentTime = LocalDate.now(zoneId)

    var result = sdf.format(this)
    if (localDate.year == currentTime.year &&
        localDate.month == currentTime.month
    ) {
        if (localDate.dayOfMonth == currentTime.dayOfMonth)
            result = "Today"
        else if (localDate.dayOfMonth == currentTime.dayOfMonth - 1)
            result = "Yesterday"
    }

    return result
}

fun Date.toSystemString(): String {
    val systemLocale = System.getProperty("user.language")
    val locale = Locale(systemLocale)
    val sdf = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)

    return sdf.format(this)
}

fun Date.toSystemDateTimeString(): String {
    val systemLocale = System.getProperty("user.language")
    val locale = Locale(systemLocale)
    val sdf = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)

    return sdf.format(this)
}