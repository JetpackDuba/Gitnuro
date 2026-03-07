package com.jetpackduba.gitnuro.domain.models

enum class AvatarProviderType(val value: Int) {
    NONE(0),
    GRAVATAR(1);

    // TODO This should be in the data layer as the domain doesn't care of how this is persisted
    companion object {
        fun fromValue(value: Int?): AvatarProviderType? {
            return when (value) {
                NONE.value -> NONE
                GRAVATAR.value -> GRAVATAR
                else -> null
            }
        }
    }
}