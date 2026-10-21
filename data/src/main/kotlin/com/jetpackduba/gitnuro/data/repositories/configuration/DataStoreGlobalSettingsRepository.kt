package com.jetpackduba.gitnuro.data.repositories.configuration

import com.jetpackduba.gitnuro.data.GlobalSettingsDataStore
import com.jetpackduba.gitnuro.data.extensions.get
import com.jetpackduba.gitnuro.data.extensions.setValue
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.autoStashOnMergePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.avatarProviderPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.cacheCredentialsPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.cloneDefaultDirectoryPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.customThemePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.dateFormatCustomFormatPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.dateFormatIs24hPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.dateFormatUseDefaultPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.dateFormatUseRelativePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.diffDisplayFullFilePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.diffTextViewTypePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.fastForwardMergePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.linesHeightPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.proxyHostNamePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.proxyHostPasswordPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.proxyHostUserPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.proxyPortNumberPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.proxyProxyTypePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.proxyUseAuthPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.pullWithRebasePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.pushWithLeasePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.scaleUiPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.showChangesAsTreePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.swapStatusPanesPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.terminalPathPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.themePreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.useProxyPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.verifySslPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.PreferencesKeys.windowPlacementPreference
import com.jetpackduba.gitnuro.data.repositories.configuration.mappers.*
import com.jetpackduba.gitnuro.data.mappers.PathsListMapper
import com.jetpackduba.gitnuro.data.repositories.ui.WindowPlacementMapper
import com.jetpackduba.gitnuro.domain.models.GlobalConfig
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.ui.WindowPlacement
import com.jetpackduba.gitnuro.domain.repositories.GlobalSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject


class DataStoreGlobalSettingsRepository @Inject constructor(
    private val dataStore: GlobalSettingsDataStore,
    private val themeMapper: ThemeMapper,
    private val linesHeightMapper: LinesHeightMapper,
    private val avatarProviderMapper: AvatarProviderMapper,
    private val textDiffViewTypeMapper: TextDiffViewTypeMapper,
    private val windowPlacementMapper: WindowPlacementMapper,
    private val pathsListMapper: PathsListMapper,
) : GlobalSettingsRepository {

    // UI
    override val scaleUi = dataStore.data[scaleUiPreference]

    override val theme = dataStore.data[themePreference].map { themeMapper.toDomain(it) }
    override val customTheme = dataStore.data[customThemePreference]

    override val linesHeightType =
        dataStore.data[linesHeightPreference].map { linesHeightMapper.toDomain(it) }

    override val dateFormatUseDefault get() = dataStore.data[dateFormatUseDefaultPreference]
    override val dateFormatCustomFormat get() = dataStore.data[dateFormatCustomFormatPreference]
    override val dateFormatIs24h get() = dataStore.data[dateFormatIs24hPreference]
    override val dateFormatUseRelative get() = dataStore.data[dateFormatUseRelativePreference]

    override val avatarProvider
        get() = dataStore.data[avatarProviderPreference].map {
            avatarProviderMapper.toDomain(
                it
            )
        }
    override val swapStatusPanes get() = dataStore.data[swapStatusPanesPreference]
    override val showChangesAsTree get() = dataStore.data[showChangesAsTreePreference]
    override val diffDisplayFullFile get() = dataStore.data[diffDisplayFullFilePreference]
    override val diffTextViewType
        get() = dataStore.data[diffTextViewTypePreference].map {
            textDiffViewTypeMapper.toDomain(
                it
            )
        }

    // Git
    override val pullWithRebase get() = dataStore.data[pullWithRebasePreference]
    override val pushWithLease get() = dataStore.data[pushWithLeasePreference]
    override val fastForwardMerge get() = dataStore.data[fastForwardMergePreference]
    override val autoStashOnMerge get() = dataStore.data[autoStashOnMergePreference]
    override val cloneDefaultDirectory get() = dataStore.data[cloneDefaultDirectoryPreference]


    // Network
    override val useProxy get() = dataStore.data[useProxyPreference]
    override val proxyUseAuth get() = dataStore.data[proxyUseAuthPreference]
    override val proxyType get() = dataStore.data[proxyProxyTypePreference].map { ProxyType.fromValue(it) }
    override val proxyHostName get() = dataStore.data[proxyHostNamePreference]
    override val proxyPortNumber get() = dataStore.data[proxyPortNumberPreference]
    override val proxyHostUser get() = dataStore.data[proxyHostUserPreference]
    override val proxyHostPassword get() = dataStore.data[proxyHostPasswordPreference]

    override val verifySsl get() = dataStore.data[verifySslPreference]
    override val cacheCredentialsInMemory get() = dataStore.data[cacheCredentialsPreference]

    // Tools
    override val terminalPath get() = dataStore.data[terminalPathPreference]

    override suspend fun setConfiguration(globalConfig: GlobalConfig) {
        dataStore.apply {
            when (globalConfig) {
                is GlobalConfig.AutoStashOnMerge -> setValue(autoStashOnMergePreference, globalConfig.value)
                is GlobalConfig.CloneDefaultDirectory -> setValue(cloneDefaultDirectoryPreference, globalConfig.value)
                is GlobalConfig.DateFormatCustomFormat -> setValue(dateFormatCustomFormatPreference, globalConfig.value)
                is GlobalConfig.DateFormatIs24h -> setValue(dateFormatIs24hPreference, globalConfig.value)
                is GlobalConfig.DateFormatUseDefault -> setValue(dateFormatUseDefaultPreference, globalConfig.value)
                is GlobalConfig.DateFormatUseRelative -> setValue(dateFormatUseRelativePreference, globalConfig.value)
                is GlobalConfig.FastForwardMerge -> setValue(fastForwardMergePreference, globalConfig.value)
                is GlobalConfig.LinesHeight -> setValue(
                    linesHeightPreference,
                    linesHeightMapper.toData(globalConfig.value)
                )

                is GlobalConfig.UseProxy -> setValue(useProxyPreference, globalConfig.value)
                is GlobalConfig.ProxyHostName -> setValue(proxyHostNamePreference, globalConfig.value)
                is GlobalConfig.ProxyHostPassword -> setValue(proxyHostPasswordPreference, globalConfig.value)
                is GlobalConfig.ProxyHost -> setValue(proxyHostUserPreference, globalConfig.value)
                is GlobalConfig.ProxyPortNumber -> setValue(proxyPortNumberPreference, globalConfig.value)
                is GlobalConfig.ProxyProxyType -> setValue(proxyProxyTypePreference, globalConfig.value.value)
                is GlobalConfig.ProxyUseAuth -> setValue(proxyUseAuthPreference, globalConfig.value)
                is GlobalConfig.PullWithRebase -> setValue(pullWithRebasePreference, globalConfig.value)
                is GlobalConfig.PushWithLease -> setValue(pushWithLeasePreference, globalConfig.value)
                is GlobalConfig.ScaleUi -> setValue(scaleUiPreference, globalConfig.value)
                is GlobalConfig.CacheCredentialsInMemory -> setValue(cacheCredentialsPreference, globalConfig.value)
                is GlobalConfig.AvatarProvider -> setValue(
                    avatarProviderPreference,
                    avatarProviderMapper.toData(globalConfig.value)
                )

                is GlobalConfig.Theme -> setValue(themePreference, themeMapper.toData(globalConfig.value))
                is GlobalConfig.CustomTheme -> setValue(customThemePreference, globalConfig.value)
                is GlobalConfig.SwapStatusPanes -> setValue(swapStatusPanesPreference, globalConfig.value)
                is GlobalConfig.DiffDisplayFullFile -> setValue(diffDisplayFullFilePreference, globalConfig.value)
                is GlobalConfig.DiffTextViewType -> setValue(
                    diffTextViewTypePreference,
                    textDiffViewTypeMapper.toData(globalConfig.value)
                )

                is GlobalConfig.ShowChangesAsTree -> setValue(showChangesAsTreePreference, globalConfig.value)
                is GlobalConfig.TerminalPath -> setValue(terminalPathPreference, globalConfig.value)
            }
        }
    }
}


