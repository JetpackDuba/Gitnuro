package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType

sealed interface GlobalConfig {
    data class ScaleUi(val value: Float?) : GlobalConfig
    data class LinesHeight(val value: LinesHeightType?) : GlobalConfig
    data class PullWithRebase(val value: Boolean) : GlobalConfig
    data class PushWithLease(val value: Boolean) : GlobalConfig
    data class FastForwardMerge(val value: Boolean) : GlobalConfig
    data class AutoStashOnMerge(val value: Boolean) : GlobalConfig
    data class UseProxy(val value: Boolean) : GlobalConfig
    data class ProxyUseAuth(val value: Boolean) : GlobalConfig
    data class ProxyProxyType(val value: ProxyType) : GlobalConfig
    data class ProxyHostName(val value: String) : GlobalConfig
    data class ProxyPortNumber(val value: Int) : GlobalConfig
    data class ProxyHost(val value: String) : GlobalConfig
    data class ProxyHostPassword(val value: String) : GlobalConfig
    data class DateFormatUseDefault(val value: Boolean) : GlobalConfig
    data class DateFormatCustomFormat(val value: String) : GlobalConfig
    data class DateFormatIs24h(val value: Boolean) : GlobalConfig
    data class DateFormatUseRelative(val value: Boolean) : GlobalConfig
    data class CloneDefaultDirectory(val value: String) : GlobalConfig
    data class CacheCredentialsInMemory(val value: Boolean) : GlobalConfig
    data class AvatarProvider(val value: AvatarProviderType) : GlobalConfig
    data class Theme(val value: com.jetpackduba.gitnuro.domain.models.ui.Theme) : GlobalConfig
    data class CustomTheme(val value: String) : GlobalConfig
    data class SwapStatusPanes(val value: Boolean) : GlobalConfig
    data class DiffDisplayFullFile(val value: Boolean) : GlobalConfig
    data class DiffTextViewType(val value: com.jetpackduba.gitnuro.domain.models.DiffTextViewType) : GlobalConfig
    data class ShowChangesAsTree(val value: Boolean) : GlobalConfig
    data class TerminalPath(val value: String) : GlobalConfig
    data class GroupBranches(val value: Boolean) : GlobalConfig
}