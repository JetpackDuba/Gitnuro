package com.jetpackduba.gitnuro.data.repositories.configuration

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.data.UserSettingsDataStore
import com.jetpackduba.gitnuro.data.repositories.configuration.mappers.AvatarProviderMapper
import com.jetpackduba.gitnuro.data.repositories.configuration.mappers.LinesHeightMapper
import com.jetpackduba.gitnuro.data.repositories.configuration.mappers.TextDiffViewTypeMapper
import com.jetpackduba.gitnuro.data.repositories.configuration.mappers.ThemeMapper
import com.jetpackduba.gitnuro.domain.models.AppConfiguration
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.ui.AppWindowPlacement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.prefs.Preferences as LegacyPreferences

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_TABS_OPENED = "latestRepositoriesTabsOpened"
private const val PREF_LATEST_REPOSITORY_TAB_SELECTED = "latestRepositoryTabSelected"
private const val PREF_LAST_OPENED_REPOSITORIES_PATH = "lastOpenedRepositoriesList"
private const val PREF_WINDOW_PLACEMENT = "windowsPlacement"
private const val PREF_FIRST_PANE_WIDTH = "firstPaneWidth"
private const val PREF_THIRD_PANE_WIDTH = "thirdPaneWidth"
private const val DEFAULT_FIRST_PANE_WIDTH = 220f
private const val DEFAULT_THIRD_PANE_WIDTH = 330f

private val scaleUiPreference get() = floatPreferencesKey("scale_ui")
private val themePreference get() = stringPreferencesKey("theme")
private val customThemePreference get() = stringPreferencesKey("custom_theme")
private val linesHeightPreference get() = stringPreferencesKey("lines_height")
private val swapStatusPanesPreference get() = booleanPreferencesKey("swap_status_panes")
private val diffDisplayFullFilePreference get() = booleanPreferencesKey("diff_display_full_file")
private val diffTextViewTypePreference get() = stringPreferencesKey("diff_text_view")
private val showChangesAsTreePreference get() = booleanPreferencesKey("show_changes_as_tree")

private val dateFormatUseDefaultPreference get() = booleanPreferencesKey("date_format_use_default")
private val dateFormatCustomFormatPreference get() = stringPreferencesKey("date_format_custom_format")
private val dateFormatIs24hPreference get() = booleanPreferencesKey("date_format_is_24h")
private val dateFormatUseRelativePreference get() = booleanPreferencesKey("date_format_use_relative")

private val avatarProviderPreference get() = stringPreferencesKey("avatar_provider")

private val fastForwardMergePreference get() = booleanPreferencesKey("fast_forward_merge")
private val autoStashOnMergePreference get() = booleanPreferencesKey("auto_stash_on_merge")
private val pullWithRebasePreference get() = booleanPreferencesKey("pull_with_rebase")
private val pushWithLeasePreference get() = booleanPreferencesKey("push_with_lease")
private val cloneDefaultDirectoryPreference get() = stringPreferencesKey("clone_default_directory")

private val useProxyPreference get() = booleanPreferencesKey("use_proxy")
private val proxyUseAuthPreference get() = booleanPreferencesKey("proxy_use_auth")
private val proxyProxyTypePreference get() = intPreferencesKey("proxy_type")
private val proxyHostNamePreference get() = stringPreferencesKey("proxy_host_name")
private val proxyPortNumberPreference get() = intPreferencesKey("proxy_port_number")
private val proxyHostUserPreference get() = stringPreferencesKey("proxy_host_user")
private val proxyHostPasswordPreference get() = stringPreferencesKey("proxy_host_password")
private val cacheCredentialsPreference get() = booleanPreferencesKey("cache_credentials_in_memory")
private val terminalPathPreference get() = stringPreferencesKey("terminal_path")

private val verifySslPreference get() = booleanPreferencesKey("verify_ssl")

operator fun <T> Flow<Preferences>.get(key: Preferences.Key<T>): Flow<T?> {
    return this.map { it[key] }
}

suspend fun <T> DataStore<Preferences>.setValue(key: Preferences.Key<T>, value: T?) {
    this.updateData {
        it.toMutablePreferences().also { preferences ->
            if (value == null) {
                preferences.remove(key)
            } else {
                preferences[key] = value
            }
        }
    }
}

@Singleton
class AppSettingsRepository @Inject constructor(
    private val userSettingsDataStore: UserSettingsDataStore,
    private val themeMapper: ThemeMapper,
    private val linesHeightMapper: LinesHeightMapper,
    private val avatarProviderMapper: AvatarProviderMapper,
    private val textDiffViewTypeMapper: TextDiffViewTypeMapper,
) {
    private val preferences = userSettingsDataStore.preferences

    // UI
    val scaleUi = preferences.data[scaleUiPreference]

    val theme = preferences.data[themePreference].map { themeMapper.map(it) }
    val customTheme = preferences.data[customThemePreference]

    val linesHeightType =
        preferences.data[linesHeightPreference].map { linesHeightMapper.map(it) } // TODO Do proper type mapping

    val dateFormatUseDefault get() = preferences.data[dateFormatUseDefaultPreference]
    val dateFormatCustomFormat get() = preferences.data[dateFormatCustomFormatPreference]
    val dateFormatIs24h get() = preferences.data[dateFormatIs24hPreference]
    val dateFormatUseRelative get() = preferences.data[dateFormatUseRelativePreference]

    val avatarProvider get() = preferences.data[avatarProviderPreference].map { avatarProviderMapper.map(it) }
    val swapStatusPanes get() = preferences.data[swapStatusPanesPreference]
    val showChangesAsTree get() = preferences.data[showChangesAsTreePreference]
    val diffDisplayFullFile get() = preferences.data[diffDisplayFullFilePreference]
    val diffTextViewType get() = preferences.data[diffTextViewTypePreference].map { textDiffViewTypeMapper.map(it) }

    // Git
    val pullWithRebase get() = preferences.data[pullWithRebasePreference]
    val pushWithLease get() = preferences.data[pushWithLeasePreference]
    val fastForwardMerge get() = preferences.data[fastForwardMergePreference]
    val autoStashOnMerge get() = preferences.data[autoStashOnMergePreference]
    val cloneDefaultDirectory get() = preferences.data[cloneDefaultDirectoryPreference]


    // Network
    val useProxy get() = preferences.data[useProxyPreference]
    val proxyUseAuth get() = preferences.data[proxyUseAuthPreference]
    val proxyType get() = preferences.data[proxyProxyTypePreference].map { ProxyType.fromValue(it) }
    val proxyHostName get() = preferences.data[proxyHostNamePreference]
    val proxyPortNumber get() = preferences.data[proxyPortNumberPreference]
    val proxyHostUser get() = preferences.data[proxyHostUserPreference]
    val proxyHostPassword get() = preferences.data[proxyHostPasswordPreference]

    val verifySsl get() = preferences.data[verifySslPreference]
    val cacheCredentialsInMemory get() = preferences.data[cacheCredentialsPreference]

    // Tools
    val terminalPath get() = preferences.data[terminalPathPreference]

    suspend fun setConfiguration(setting: AppConfiguration) {
        preferences.apply {
            when (setting) {
                is AppConfiguration.AutoStashOnMerge -> setValue(autoStashOnMergePreference, setting.value)
                is AppConfiguration.CloneDefaultDirectory -> setValue(cloneDefaultDirectoryPreference, setting.value)
                is AppConfiguration.DateFormatCustomFormat -> setValue(dateFormatCustomFormatPreference, setting.value)
                is AppConfiguration.DateFormatIs24h -> setValue(dateFormatIs24hPreference, setting.value)
                is AppConfiguration.DateFormatUseDefault -> setValue(dateFormatUseDefaultPreference, setting.value)
                is AppConfiguration.DateFormatUseRelative -> setValue(dateFormatUseRelativePreference, setting.value)
                is AppConfiguration.FastForwardMerge -> setValue(fastForwardMergePreference, setting.value)
                is AppConfiguration.LinesHeight -> setValue(linesHeightPreference, linesHeightMapper.map(setting.value))
                is AppConfiguration.ProxyHostName -> setValue(proxyHostNamePreference, setting.value)
                is AppConfiguration.ProxyHostPassword -> setValue(proxyHostPasswordPreference, setting.value)
                is AppConfiguration.ProxyHostApp -> setValue(proxyHostUserPreference, setting.value)
                is AppConfiguration.ProxyPortNumber -> setValue(proxyPortNumberPreference, setting.value)
                is AppConfiguration.ProxyProxyType -> setValue(proxyProxyTypePreference, setting.value.value)
                is AppConfiguration.ProxyUseAuth -> setValue(proxyUseAuthPreference, setting.value)
                is AppConfiguration.PullWithRebase -> setValue(pullWithRebasePreference, setting.value)
                is AppConfiguration.PushWithLease -> setValue(pushWithLeasePreference, setting.value)
                is AppConfiguration.ScaleUi -> setValue(scaleUiPreference, setting.value)
                is AppConfiguration.CacheCredentialsInMemory -> setValue(cacheCredentialsPreference, setting.value)
                is AppConfiguration.AvatarProvider -> setValue(
                    avatarProviderPreference,
                    avatarProviderMapper.map(setting.value)
                )

                is AppConfiguration.Theme -> setValue(themePreference, themeMapper.map(setting.value))
                is AppConfiguration.CustomTheme -> setValue(customThemePreference, setting.value)
                is AppConfiguration.SwapStatusPanes -> setValue(swapStatusPanesPreference, setting.value)
                is AppConfiguration.DiffDisplayFullFile -> setValue(diffDisplayFullFilePreference, setting.value)
                is AppConfiguration.DiffTextViewType -> setValue(diffTextViewTypePreference, textDiffViewTypeMapper.map(setting.value))
                is AppConfiguration.ShowChangesAsTree -> setValue(diffDisplayFullFilePreference, setting.value)
            }
        }
    }

    private val preferencesLegacy: LegacyPreferences = LegacyPreferences.userRoot().node(PREFERENCES_NAME)

    var latestTabsOpened: String
        get() = preferencesLegacy.get(PREF_LATEST_REPOSITORIES_TABS_OPENED, "")
        set(value) {
            preferencesLegacy.put(PREF_LATEST_REPOSITORIES_TABS_OPENED, value)
        }

    var latestRepositoryTabSelected: Int
        get() = preferencesLegacy.getInt(PREF_LATEST_REPOSITORY_TAB_SELECTED, -1)
        set(value) {
            preferencesLegacy.putInt(PREF_LATEST_REPOSITORY_TAB_SELECTED, value)
        }

    var latestOpenedRepositoriesPath: String
        get() = preferencesLegacy.get(PREF_LAST_OPENED_REPOSITORIES_PATH, "")
        set(value) {
            preferencesLegacy.put(PREF_LAST_OPENED_REPOSITORIES_PATH, value)
        }

    var firstPaneWidth: Float
        get() {
            return preferencesLegacy.getFloat(PREF_FIRST_PANE_WIDTH, DEFAULT_FIRST_PANE_WIDTH)
        }
        set(value) {
            preferencesLegacy.putFloat(PREF_FIRST_PANE_WIDTH, value)
        }

    var thirdPaneWidth: Float
        get() {
            return preferencesLegacy.getFloat(PREF_THIRD_PANE_WIDTH, DEFAULT_THIRD_PANE_WIDTH)
        }
        set(value) {
            preferencesLegacy.putFloat(PREF_THIRD_PANE_WIDTH, value)
        }


    var windowPlacement: AppWindowPlacement
        get() {
            // FIXME Preference doing nothing here
            val placement = preferencesLegacy.getInt(PREF_WINDOW_PLACEMENT, 0)

            return AppWindowPlacement.MAXIMIZED
        }
        set(placement) {
            //preferences.putInt(PREF_WINDOW_PLACEMENT, placement.value)
        }
}

fun initPreferencesPath() {
    if (currentOs == OS.LINUX) {
        val xdgConfigHome: String? = System.getenv("XDG_CONFIG_HOME")

        val settingsPath = if (xdgConfigHome.isNullOrBlank()) {
            val home = System.getProperty("user.home").orEmpty()
            "$home/.config/gitnuro"
        } else {
            "$xdgConfigHome/gitnuro"
        }

        System.setProperty("java.util.prefs.userRoot", settingsPath)
    }
}

// TODO verify this after refactor. Are the paths for mac and windows correct?
fun getPreferencesPath(): String {
    val home = System.getProperty("user.home").orEmpty()

    return when (currentOs) {
        OS.LINUX -> {
            val xdgConfigHome: String? = System.getenv("XDG_CONFIG_HOME")

            val settingsPath = if (xdgConfigHome.isNullOrBlank()) {
                "$home/.config/gitnuro"
            } else {
                "$xdgConfigHome/gitnuro"
            }

            settingsPath
        }

        OS.MAC -> {
            "$home/Library/Application Support/gitnuro"
        }

        else -> {
            System.getProperty("java.util.prefs.userRoot") ?: "$home${systemSeparator}gitnuro"
        }
    } + systemSeparator + "user_prefs.preferences_pb"
}