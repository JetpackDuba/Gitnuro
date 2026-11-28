package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.AppLanguage
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import javax.inject.Inject

private const val SYSTEM_DEFAULT = "system_default"
private const val ENGLISH = "english"
private const val SPANISH = "spanish"
private const val CATALAN = "catalan"

class AppLanguageMapper @Inject constructor() : DataMapper<AppLanguage?, String?> {
    override fun toData(value: AppLanguage?): String? {
        return when (value) {
            AppLanguage.SYSTEM_DEFAULT -> SYSTEM_DEFAULT
            AppLanguage.ENGLISH -> ENGLISH
            AppLanguage.SPANISH -> SPANISH
            AppLanguage.CATALAN -> CATALAN
            null -> null
        }
    }


    override fun toDomain(value: String?): AppLanguage? {
        return when (value) {
            SYSTEM_DEFAULT -> AppLanguage.SYSTEM_DEFAULT
            ENGLISH -> AppLanguage.ENGLISH
            SPANISH -> AppLanguage.SPANISH
            CATALAN -> AppLanguage.CATALAN
            else -> null
        }
    }
}