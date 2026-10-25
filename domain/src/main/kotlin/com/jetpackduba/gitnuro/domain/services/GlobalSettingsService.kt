package com.jetpackduba.gitnuro.domain.services

import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.common.flows.defaultIfNull
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.DiffTextViewType
import com.jetpackduba.gitnuro.domain.models.GlobalConfig
import com.jetpackduba.gitnuro.domain.models.ProxySettings
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import com.jetpackduba.gitnuro.domain.repositories.GlobalSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GlobalSettingsService @Inject constructor(
    private val globalSettingsRepository: GlobalSettingsRepository,
) {
    suspend fun setConfiguration(globalConfig: GlobalConfig) {
        globalSettingsRepository.setConfiguration(globalConfig)
    }

    val scaleUi: Flow<Float?> get() = globalSettingsRepository.scaleUi
    val theme: Flow<Theme> get() = globalSettingsRepository.theme.defaultIfNull { DEFAULT_THEME }
    val customTheme: Flow<String?> get() = globalSettingsRepository.customTheme
    val linesHeightType: Flow<LinesHeightType> get() = globalSettingsRepository.linesHeightType.defaultIfNull { DEFAULT_LINES_HEIGHT }
    val dateFormatUseDefault: Flow<Boolean> get() = globalSettingsRepository.dateFormatUseDefault.defaultIfNull { DEFAULT_DATE_USE_DEFAULT }
    val dateFormatCustomFormat: Flow<String> get() = globalSettingsRepository.dateFormatCustomFormat.defaultIfNull { DEFAULT_DATE_CUSTOM_FORMAT }
    val dateFormatIs24h: Flow<Boolean> get() = globalSettingsRepository.dateFormatIs24h.defaultIfNull { DEFAULT_DATE_IS_24H }
    val dateFormatUseRelative: Flow<Boolean> get() = globalSettingsRepository.dateFormatUseRelative.defaultIfNull { DEFAULT_DATE_USE_RELATIVE }
    val avatarProvider: Flow<AvatarProviderType> get() = globalSettingsRepository.avatarProvider.defaultIfNull { DEFAULT_AVATAR_PROVIDER }
    val swapStatusPanes: Flow<Boolean> get() = globalSettingsRepository.swapStatusPanes.defaultIfNull { DEFAULT_SWAP_STATUS_PANES }
    val showChangesAsTree: Flow<Boolean> get() = globalSettingsRepository.showChangesAsTree.defaultIfNull { DEFAULT_SHOW_CHANGES_AS_TREE }
    val diffDisplayFullFile: Flow<Boolean> get() = globalSettingsRepository.diffDisplayFullFile.defaultIfNull { DEFAULT_DIFF_DISPLAY_FULL_FILE }
    val diffTextViewType: Flow<DiffTextViewType> get() = globalSettingsRepository.diffTextViewType.defaultIfNull { DEFAULT_DIFF_TEXT_VIEW_TYPE }
    val pullWithRebase: Flow<Boolean> get() = globalSettingsRepository.pullWithRebase.defaultIfNull { DEFAULT_PULL_WITH_REBASE }
    val pushWithLease: Flow<Boolean> get() = globalSettingsRepository.pushWithLease.defaultIfNull { DEFAULT_PUSH_WITH_LEASE }
    val fastForwardMerge: Flow<Boolean> get() = globalSettingsRepository.fastForwardMerge.defaultIfNull { DEFAULT_FAST_FORWARD_MERGE }
    val autoStashOnMerge: Flow<Boolean> get() = globalSettingsRepository.autoStashOnMerge.defaultIfNull { DEFAULT_AUTO_STASH_ON_MERGE }
    val cloneDefaultDirectory: Flow<String?> get() = globalSettingsRepository.cloneDefaultDirectory
    val useProxy: Flow<Boolean> get() = globalSettingsRepository.useProxy.defaultIfNull { DEFAULT_USE_PROXY }
    val proxyUseAuth: Flow<Boolean> get() = globalSettingsRepository.proxyUseAuth.defaultIfNull { DEFAULT_PROXY_USE_AUTH }
    val proxyType: Flow<ProxyType> get() = globalSettingsRepository.proxyType.defaultIfNull { DEFAULT_PROXY_TYPE }
    val proxyHostName: Flow<String?> get() = globalSettingsRepository.proxyHostName
    val proxyPortNumber: Flow<Int?> get() = globalSettingsRepository.proxyPortNumber
    val proxyHostUser: Flow<String?> get() = globalSettingsRepository.proxyHostUser
    val proxyHostPassword: Flow<String?> get() = globalSettingsRepository.proxyHostPassword
    val verifySsl: Flow<Boolean> get() = globalSettingsRepository.verifySsl.defaultIfNull { DEFAULT_VERIFY_SSL }
    val cacheCredentialsInMemory: Flow<Boolean> get() = globalSettingsRepository.cacheCredentialsInMemory.defaultIfNull { DEFAULT_CACHE_CREDENTIALS_IN_MEMORY }
    val terminalPath: Flow<String?> get() = globalSettingsRepository.terminalPath
    val groupBranchesWithRemotes: Flow<Boolean> get() = globalSettingsRepository.groupBranchesWithRemotes.defaultIfNull { DEFAULT_GROUP_BRANCHES_WITH_REMOTES }

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
        const val DEFAULT_GROUP_BRANCHES_WITH_REMOTES = true
    }
}