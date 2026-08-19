package com.jetpackduba.gitnuro.domain.models.ui

/**
 * UI language preference.
 *
 * [code] is persisted in DataStore. Use BCP-47 language tags for concrete
 * locales (e.g. "en", "ru", "uk"). [System] follows the JVM default locale
 * captured at process start.
 */
enum class AppLanguage(val code: String, val displayName: String) {
    System("system", "System OS language"),
    English("en", "English"),
    Russian("ru", "Русский"),
    ;

    companion object {
        fun fromCode(code: String?): AppLanguage {
            if (code.isNullOrBlank()) return System
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: System
        }
    }
}
