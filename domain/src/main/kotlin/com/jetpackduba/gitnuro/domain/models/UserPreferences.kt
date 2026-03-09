package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType

sealed interface UserPreferences {
    data class ScaleUi(val value: Float?) : UserPreferences
    data class LinesHeight(val value: LinesHeightType?) : UserPreferences
    data class PullWithRebase(val value: Boolean) : UserPreferences
    data class PushWithLease(val value: Boolean) : UserPreferences
    data class FastForwardMerge(val value: Boolean) : UserPreferences
    data class AutoStashOnMerge(val value: Boolean) : UserPreferences
    data class ProxyUseAuth(val value: Boolean) : UserPreferences
    data class ProxyProxyType(val value: ProxyType) : UserPreferences
    data class ProxyHostName(val value: String) : UserPreferences
    data class ProxyPortNumber(val value: Int) : UserPreferences
    data class ProxyHostUser(val value: String) : UserPreferences
    data class ProxyHostPassword(val value: String) : UserPreferences
    data class DateFormatUseDefault(val value: Boolean) : UserPreferences
    data class DateFormatCustomFormat(val value: String) : UserPreferences
    data class DateFormatIs24h(val value: Boolean) : UserPreferences
    data class DateFormatUseRelative(val value: Boolean) : UserPreferences
    data class CloneDefaultDirectory(val value: String) : UserPreferences
    data class CacheCredentialsInMemory(val value: Boolean) : UserPreferences
    data class AvatarProvider(val value: AvatarProviderType) : UserPreferences
}