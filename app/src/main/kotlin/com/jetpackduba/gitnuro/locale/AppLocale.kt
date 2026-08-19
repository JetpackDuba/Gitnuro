package com.jetpackduba.gitnuro.locale

import com.jetpackduba.gitnuro.domain.models.ui.AppLanguage
import java.util.Locale

/**
 * Captures the process-start JVM locale so [AppLanguage.System] can restore it
 * after the user previously selected a fixed language.
 */
private val systemLocale: Locale = Locale.getDefault()

fun applyAppLanguage(language: AppLanguage) {
    val locale = when (language) {
        AppLanguage.System -> systemLocale
        else -> Locale.forLanguageTag(language.code)
    }
    Locale.setDefault(locale)
}

fun currentAppLocale(language: AppLanguage): Locale = when (language) {
    AppLanguage.System -> systemLocale
    else -> Locale.forLanguageTag(language.code)
}
