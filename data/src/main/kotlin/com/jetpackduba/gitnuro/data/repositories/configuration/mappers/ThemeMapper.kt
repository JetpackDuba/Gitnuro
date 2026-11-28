package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import javax.inject.Inject

private const val SYSTEM_DEFAULT = "system_default"
private const val DARK = "dark"
private const val LIGHT = "light"
private const val CUSTOM = "custom"

class ThemeMapper @Inject constructor() : DataMapper<Theme?, String?> {
    override fun toData(value: Theme?): String? {
        return when (value) {
            Theme.SYSTEM_DEFAULT -> SYSTEM_DEFAULT
            Theme.LIGHT -> LIGHT
            Theme.DARK -> DARK
            Theme.CUSTOM -> CUSTOM
            null -> null
        }
    }


    override fun toDomain(value: String?): Theme? {
        return when (value) {
            SYSTEM_DEFAULT -> Theme.SYSTEM_DEFAULT
            LIGHT -> Theme.LIGHT
            DARK -> Theme.DARK
            CUSTOM -> Theme.CUSTOM
            else -> null
        }
    }
}