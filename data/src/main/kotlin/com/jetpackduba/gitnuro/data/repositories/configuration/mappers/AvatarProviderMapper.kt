package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType

private const val NONE = "none"
private const val GRAVATAR = "gravatar"

class AvatarProviderMapper: DataMapper<AvatarProviderType?, String?>  {
    override fun map(value: AvatarProviderType?): String? {
        return when (value) {
            AvatarProviderType.None -> NONE
            AvatarProviderType.Gravatar -> GRAVATAR
            null -> null
        }
    }

    override fun map(value: String?): AvatarProviderType? {
        return when (value) {
            NONE -> AvatarProviderType.None
            GRAVATAR -> AvatarProviderType.Gravatar
            null -> null
            else -> throw IllegalStateException("Unhandled avatar provider $value")
        }
    }
}
