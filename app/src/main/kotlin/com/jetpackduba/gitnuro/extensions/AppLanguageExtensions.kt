package com.jetpackduba.gitnuro.extensions

import com.jetpackduba.gitnuro.domain.models.AppLanguage
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun AppLanguage.toLocaleValue(): String? {
    return when (this) {
        AppLanguage.SYSTEM_DEFAULT -> null
        AppLanguage.ENGLISH -> "en"
        AppLanguage.SPANISH -> "es"
        AppLanguage.CATALAN -> "ca"
    }
}