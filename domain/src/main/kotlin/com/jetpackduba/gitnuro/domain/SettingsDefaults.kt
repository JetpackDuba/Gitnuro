package com.jetpackduba.gitnuro.domain

import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.DateTimeFormat

object SettingsDefaults {
    val defaultAvatarProviderType = AvatarProviderType.Gravatar
    val defaultDateTimeFormat = DateTimeFormat(
        useSystemDefault = true,
        customFormat = "dd MMM yyyy",
        is24hours = true,
        useRelativeDate = true,
    )
}