package com.jetpackduba.gitnuro.domain.services

import com.jetpackduba.gitnuro.common.flows.defaultIfNull
import com.jetpackduba.gitnuro.domain.models.AppConfig
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.DiffTextViewType
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import com.jetpackduba.gitnuro.domain.repositories.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AppSettingsService @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend fun setConfiguration(appConfig: AppConfig) {
        appSettingsRepository.setConfiguration(appConfig)
    }

    val scaleUi: Flow<Float?> get() = appSettingsRepository.scaleUi
    val theme: Flow<Theme> get() = appSettingsRepository.theme.defaultIfNull { DEFAULT_THEME }
    val customTheme: Flow<String?> get() = appSettingsRepository.customTheme
    val linesHeightType: Flow<LinesHeightType> get() = appSettingsRepository.linesHeightType.defaultIfNull { DEFAULT_LINES_HEIGHT }
    val dateFormatUseDefault: Flow<Boolean> get() = appSettingsRepository.dateFormatUseDefault.defaultIfNull { DEFAULT_DATE_USE_DEFAULT }
    val dateFormatCustomFormat: Flow<String> get() = appSettingsRepository.dateFormatCustomFormat.defaultIfNull { DEFAULT_DATE_CUSTOM_FORMAT }
    val dateFormatIs24h: Flow<Boolean> get() = appSettingsRepository.dateFormatIs24h.defaultIfNull { DEFAULT_DATE_IS_24H }
    val dateFormatUseRelative: Flow<Boolean> get() = appSettingsRepository.dateFormatUseRelative.defaultIfNull { DEFAULT_DATE_USE_RELATIVE }
    val avatarProvider: Flow<AvatarProviderType> get() = appSettingsRepository.avatarProvider.defaultIfNull { DEFAULT_AVATAR_PROVIDER }
    val swapStatusPanes: Flow<Boolean> get() = appSettingsRepository.swapStatusPanes.defaultIfNull { DEFAULT_SWAP_STATUS_PANES }
    val showChangesAsTree: Flow<Boolean> get() = appSettingsRepository.showChangesAsTree.defaultIfNull { DEFAULT_SHOW_CHANGES_AS_TREE }
    val diffDisplayFullFile: Flow<Boolean> get() = appSettingsRepository.diffDisplayFullFile.defaultIfNull { DEFAULT_DIFF_DISPLAY_FULL_FILE }
    val diffTextViewType: Flow<DiffTextViewType> get() = appSettingsRepository.diffTextViewType.defaultIfNull { DEFAULT_DIFF_TEXT_VIEW_TYPE }
    val pullWithRebase: Flow<Boolean> get() = appSettingsRepository.pullWithRebase.defaultIfNull { DEFAULT_PULL_WITH_REBASE }
    val pushWithLease: Flow<Boolean> get() = appSettingsRepository.pushWithLease.defaultIfNull { DEFAULT_PUSH_WITH_LEASE }
    val fastForwardMerge: Flow<Boolean> get() = appSettingsRepository.fastForwardMerge.defaultIfNull { DEFAULT_FAST_FORWARD_MERGE }
    val autoStashOnMerge: Flow<Boolean> get() = appSettingsRepository.autoStashOnMerge.defaultIfNull { DEFAULT_AUTO_STASH_ON_MERGE }
    val cloneDefaultDirectory: Flow<String?> get() = appSettingsRepository.cloneDefaultDirectory
    val useProxy: Flow<Boolean> get() = appSettingsRepository.useProxy.defaultIfNull { DEFAULT_USE_PROXY }
    val proxyUseAuth: Flow<Boolean> get() = appSettingsRepository.proxyUseAuth.defaultIfNull { DEFAULT_PROXY_USE_AUTH }
    val proxyType: Flow<ProxyType> get() = appSettingsRepository.proxyType.defaultIfNull { DEFAULT_PROXY_TYPE }
    val proxyHostName: Flow<String?> get() = appSettingsRepository.proxyHostName
    val proxyPortNumber: Flow<Int?> get() = appSettingsRepository.proxyPortNumber
    val proxyHostUser: Flow<String?> get() = appSettingsRepository.proxyHostUser
    val proxyHostPassword: Flow<String?> get() = appSettingsRepository.proxyHostPassword
    val verifySsl: Flow<Boolean> get() = appSettingsRepository.verifySsl.defaultIfNull { DEFAULT_VERIFY_SSL }
    val cacheCredentialsInMemory: Flow<Boolean> get() = appSettingsRepository.cacheCredentialsInMemory.defaultIfNull { DEFAULT_CACHE_CREDENTIALS_IN_MEMORY }
    val terminalPath: Flow<String?> get() = appSettingsRepository.terminalPath

    companion object {
        val DEFAULT_THEME = Theme.Dark
        val DEFAULT_LINES_HEIGHT = LinesHeightType.SPACED
        const val DEFAULT_DATE_USE_DEFAULT = true
        const val DEFAULT_DATE_IS_24H = true
        const val DEFAULT_DATE_USE_RELATIVE = true
        const val DEFAULT_DATE_CUSTOM_FORMAT = "dd MMM yyyy"
        val DEFAULT_AVATAR_PROVIDER = AvatarProviderType.Gravatar
        const val DEFAULT_SWAP_STATUS_PANES = false
        const val DEFAULT_SHOW_CHANGES_AS_TREE = false
        const val DEFAULT_DIFF_DISPLAY_FULL_FILE = false
        val DEFAULT_DIFF_TEXT_VIEW_TYPE = DiffTextViewType.Unified
        const val DEFAULT_PULL_WITH_REBASE = false
        const val DEFAULT_PUSH_WITH_LEASE = true
        const val DEFAULT_FAST_FORWARD_MERGE = true
        const val DEFAULT_AUTO_STASH_ON_MERGE = true
        const val DEFAULT_USE_PROXY = false
        const val DEFAULT_PROXY_USE_AUTH = false
        val DEFAULT_PROXY_TYPE = ProxyType.HTTP
        const val DEFAULT_VERIFY_SSL = true
        const val DEFAULT_CACHE_CREDENTIALS_IN_MEMORY = true
    }
}