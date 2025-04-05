package com.jetpackduba.gitnuro.preferences

import com.jetpackduba.gitnuro.SettingsDefaults

enum class AvatarProviderType(val value: Int) {
    NONE(0),
    GRAVATAR(1);

    companion object {
        fun getFromValue(value: Int): AvatarProviderType {
            return when (value) {
                NONE.value -> NONE
                GRAVATAR.value -> GRAVATAR
                else -> SettingsDefaults.defaultAvatarProviderType
            }
        }
    }
}