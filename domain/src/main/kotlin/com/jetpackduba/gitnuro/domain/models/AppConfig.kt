package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType

sealed interface AppConfig {
    data class ScaleUi(val value: Float?) : AppConfig
    data class LinesHeight(val value: LinesHeightType?) : AppConfig
    data class PullWithRebase(val value: Boolean) : AppConfig
    data class PushWithLease(val value: Boolean) : AppConfig
    data class FastForwardMerge(val value: Boolean) : AppConfig
    data class AutoStashOnMerge(val value: Boolean) : AppConfig
    data class UseProxy(val value: Boolean) : AppConfig
    data class ProxyUseAuth(val value: Boolean) : AppConfig
    data class ProxyProxyType(val value: ProxyType) : AppConfig
    data class ProxyHostName(val value: String) : AppConfig
    data class ProxyPortNumber(val value: Int) : AppConfig
    data class ProxyHostUser(val value: String) : AppConfig
    data class ProxyHostPassword(val value: String) : AppConfig
    data class DateFormatUseDefault(val value: Boolean) : AppConfig
    data class DateFormatCustomFormat(val value: String) : AppConfig
    data class DateFormatIs24h(val value: Boolean) : AppConfig
    data class DateFormatUseRelative(val value: Boolean) : AppConfig
    data class CloneDefaultDirectory(val value: String) : AppConfig
    data class CacheCredentialsInMemory(val value: Boolean) : AppConfig
    data class AvatarProvider(val value: AvatarProviderType) : AppConfig
    data class Theme(val value: com.jetpackduba.gitnuro.domain.models.ui.Theme) : AppConfig
    data class CustomTheme(val value: String) : AppConfig
    data class SwapStatusPanes(val value: Boolean) : AppConfig
    data class DiffDisplayFullFile(val value: Boolean) : AppConfig
    data class DiffTextViewType(val value: com.jetpackduba.gitnuro.domain.models.DiffTextViewType) : AppConfig
    data class ShowChangesAsTree(val value: Boolean) : AppConfig
    data class TerminalPath(val value: String) : AppConfig
}