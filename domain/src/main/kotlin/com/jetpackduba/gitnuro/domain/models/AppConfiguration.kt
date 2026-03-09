package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType

sealed interface AppConfiguration {
    data class ScaleUi(val value: Float?) : AppConfiguration
    data class LinesHeight(val value: LinesHeightType?) : AppConfiguration
    data class PullWithRebase(val value: Boolean) : AppConfiguration
    data class PushWithLease(val value: Boolean) : AppConfiguration
    data class FastForwardMerge(val value: Boolean) : AppConfiguration
    data class AutoStashOnMerge(val value: Boolean) : AppConfiguration
    data class ProxyUseAuth(val value: Boolean) : AppConfiguration
    data class ProxyProxyType(val value: ProxyType) : AppConfiguration
    data class ProxyHostName(val value: String) : AppConfiguration
    data class ProxyPortNumber(val value: Int) : AppConfiguration
    data class ProxyHostApp(val value: String) : AppConfiguration
    data class ProxyHostPassword(val value: String) : AppConfiguration
    data class DateFormatUseDefault(val value: Boolean) : AppConfiguration
    data class DateFormatCustomFormat(val value: String) : AppConfiguration
    data class DateFormatIs24h(val value: Boolean) : AppConfiguration
    data class DateFormatUseRelative(val value: Boolean) : AppConfiguration
    data class CloneDefaultDirectory(val value: String) : AppConfiguration
    data class CacheCredentialsInMemory(val value: Boolean) : AppConfiguration
    data class AvatarProvider(val value: AvatarProviderType) : AppConfiguration
    data class Theme(val value: com.jetpackduba.gitnuro.domain.models.ui.Theme) : AppConfiguration
    data class CustomTheme(val value: String) : AppConfiguration
    data class SwapStatusPanes(val value: Boolean) : AppConfiguration
    data class DiffDisplayFullFile(val value: Boolean) : AppConfiguration
    data class DiffTextViewType(val value: com.jetpackduba.gitnuro.domain.models.DiffTextViewType) : AppConfiguration
    data class ShowChangesAsTree(val value: Boolean) : AppConfiguration
}