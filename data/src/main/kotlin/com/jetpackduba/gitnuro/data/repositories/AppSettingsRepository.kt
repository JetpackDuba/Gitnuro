package com.jetpackduba.gitnuro.data.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.data.UserSettingsDataStore
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.models.ui.AppWindowPlacement
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.util.prefs.Preferences as LegacyPreferences

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_TABS_OPENED = "latestRepositoriesTabsOpened"
private const val PREF_LATEST_REPOSITORY_TAB_SELECTED = "latestRepositoryTabSelected"
private const val PREF_LAST_OPENED_REPOSITORIES_PATH = "lastOpenedRepositoriesList"
private const val PREF_THEME = "theme"
private const val PREF_WINDOW_PLACEMENT = "windowsPlacement"
private const val PREF_CUSTOM_THEME = "customTheme"
private const val PREF_DIFF_TYPE = "diffType"
private const val PREF_DIFF_FULL_FILE = "diffFullFile"
private const val PREF_SWAP_UNCOMMITTED_CHANGES = "inverseUncommittedChanges"
private const val PREF_TERMINAL_PATH = "terminalPath"
private const val PREF_SHOW_CHANGES_AS_TREE = "showChangesAsTree"
private const val PREF_AVATAR_PROVIDER = "avatarProvider"
private const val PREF_CACHE_CREDENTIALS_IN_MEMORY = "credentialsInMemory"
private const val PREF_FIRST_PANE_WIDTH = "firstPaneWidth"
private const val PREF_THIRD_PANE_WIDTH = "thirdPaneWidth"

private const val PREF_GIT_DEFAULT_CLONE_DIR = "gitDefaultCloneDir"

private const val DEFAULT_SWAP_UNCOMMITTED_CHANGES = false
private const val DEFAULT_SHOW_CHANGES_AS_TREE = false
private const val DEFAULT_CACHE_CREDENTIALS_IN_MEMORY = true
private const val DEFAULT_VERIFY_SSL = true
private const val DEFAULT_FIRST_PANE_WIDTH = 220f
private const val DEFAULT_THIRD_PANE_WIDTH = 330f
const val DEFAULT_UI_SCALE = -1f

private val scaleUiPreference get() = floatPreferencesKey("scale_ui")
private val linesHeightTypePreference get() = intPreferencesKey("lines_height")

private val dateFormatUseDefaultPreference get() = booleanPreferencesKey("date_format_use_default")
private val dateFormatCustomFormatPreference get() = stringPreferencesKey("date_format_custom_format")
private val dateFormatIs24hPreference get() = booleanPreferencesKey("date_format_is_24h")
private val dateFormatUseRelativePreference get() = booleanPreferencesKey("date_format_use_relative")
private val avatarProviderPreference get() = intPreferencesKey("avatar_provider")

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
) {
    private val preferences = userSettingsDataStore.preferences

    // UI
    val scaleUi get() = preferences.data[scaleUiPreference]

    val linesHeightType =
        preferences.data[linesHeightTypePreference].map { LinesHeightType.fromValue(it) } // TODO Do proper type mapping

    val dateFormatUseDefault = preferences.data[dateFormatUseDefaultPreference]
    val dateFormatCustomFormat = preferences.data[dateFormatCustomFormatPreference]
    val dateFormatIs24h = preferences.data[dateFormatIs24hPreference]
    val dateFormatUseRelative = preferences.data[dateFormatUseRelativePreference]

    val avatarProvider = preferences.data[avatarProviderPreference].map { AvatarProviderType.fromValue(it) }

    // Git
    val pullWithRebase = preferences.data[pullWithRebasePreference]
    val pushWithLease = preferences.data[pushWithLeasePreference]
    val fastForwardMerge = preferences.data[fastForwardMergePreference]
    val autoStashOnMerge = preferences.data[autoStashOnMergePreference]
    val cloneDefaultDirectory = preferences.data[cloneDefaultDirectoryPreference]


    // Network
    val useProxy = preferences.data[useProxyPreference]
    val proxyUseAuth = preferences.data[proxyUseAuthPreference]
    val proxyType = preferences.data[proxyProxyTypePreference].map { ProxyType.fromValue(it) }
    val proxyHostName = preferences.data[proxyHostNamePreference]
    val proxyPortNumber = preferences.data[proxyPortNumberPreference]
    val proxyHostUser = preferences.data[proxyHostUserPreference]
    val proxyHostPassword = preferences.data[proxyHostPasswordPreference]

    val verifySsl = preferences.data[verifySslPreference]
    val cacheCredentialsInMemory = preferences.data[cacheCredentialsPreference]

    // Tools
    val terminalPath = preferences.data[terminalPathPreference]

    suspend fun setPreference(setting: UserPreferences) {
        preferences.apply {
            when (setting) {
                is UserPreferences.AutoStashOnMerge -> setValue(autoStashOnMergePreference, setting.value)
                is UserPreferences.CloneDefaultDirectory -> setValue(cloneDefaultDirectoryPreference, setting.value)
                is UserPreferences.DateFormatCustomFormat -> setValue(dateFormatCustomFormatPreference, setting.value)
                is UserPreferences.DateFormatIs24h -> setValue(dateFormatIs24hPreference, setting.value)
                is UserPreferences.DateFormatUseDefault -> setValue(dateFormatUseDefaultPreference, setting.value)
                is UserPreferences.DateFormatUseRelative -> setValue(dateFormatUseRelativePreference, setting.value)
                is UserPreferences.FastForwardMerge -> setValue(fastForwardMergePreference, setting.value)
                is UserPreferences.LinesHeight -> setValue(linesHeightTypePreference, setting.value?.value)
                is UserPreferences.ProxyHostName -> setValue(proxyHostNamePreference, setting.value)
                is UserPreferences.ProxyHostPassword -> setValue(proxyHostPasswordPreference, setting.value)
                is UserPreferences.ProxyHostUser -> setValue(proxyHostUserPreference, setting.value)
                is UserPreferences.ProxyPortNumber -> setValue(proxyPortNumberPreference, setting.value)
                is UserPreferences.ProxyProxyType -> setValue(proxyProxyTypePreference, setting.value.value)
                is UserPreferences.ProxyUseAuth -> setValue(proxyUseAuthPreference, setting.value)
                is UserPreferences.PullWithRebase -> setValue(pullWithRebasePreference, setting.value)
                is UserPreferences.PushWithLease -> setValue(pushWithLeasePreference, setting.value)
                is UserPreferences.ScaleUi -> setValue(scaleUiPreference, setting.value)
                is UserPreferences.CacheCredentialsInMemory -> setValue(cacheCredentialsPreference, setting.value)
                is UserPreferences.AvatarProvider -> setValue(avatarProviderPreference, setting.value.value)
            }
        }
    }

    private val preferencesLegacy: LegacyPreferences = LegacyPreferences.userRoot().node(PREFERENCES_NAME)

    private val _themeState = MutableStateFlow(theme)
    val themeState = _themeState.asStateFlow()

    private val _swapUncommittedChangesFlow = MutableStateFlow(swapUncommittedChanges)
    val swapUncommittedChangesFlow = _swapUncommittedChangesFlow.asStateFlow()

    private val _showChangesAsTreeFlow = MutableStateFlow(showChangesAsTree)
    val showChangesAsTreeFlow = _showChangesAsTreeFlow.asStateFlow()

//    private val _customThemeFlow = MutableStateFlow<ColorsScheme?>(null)
//    val customThemeFlow = _customThemeFlow.asStateFlow()

    private val _textDiffTypeFlow = MutableStateFlow(textDiffType)
    val textDiffTypeFlow = _textDiffTypeFlow.asStateFlow()

    private val _textDiffFullFileFlow = MutableStateFlow(diffDisplayFullFile)
    val diffDisplayFullFileFlow = _textDiffFullFileFlow.asStateFlow()

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

    var theme: Theme
        get() {
            val lastTheme = preferencesLegacy.get(PREF_THEME, Theme.DARK.toString())
            return try {
                Theme.valueOf(lastTheme)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Theme.DARK
            }
        }
        set(value) {
            preferencesLegacy.put(PREF_THEME, value.toString())
            _themeState.value = value
        }

    var swapUncommittedChanges: Boolean
        get() {
            return preferencesLegacy.getBoolean(PREF_SWAP_UNCOMMITTED_CHANGES, DEFAULT_SWAP_UNCOMMITTED_CHANGES)
        }
        set(value) {
            preferencesLegacy.putBoolean(PREF_SWAP_UNCOMMITTED_CHANGES, value)
            _swapUncommittedChangesFlow.value = value
        }

    var showChangesAsTree: Boolean
        get() {
            return preferencesLegacy.getBoolean(PREF_SHOW_CHANGES_AS_TREE, DEFAULT_SHOW_CHANGES_AS_TREE)
        }
        set(value) {
            preferencesLegacy.putBoolean(PREF_SHOW_CHANGES_AS_TREE, value)
            _showChangesAsTreeFlow.value = value
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

    var textDiffType: TextDiffType
        get() {
            val diffTypeValue = preferencesLegacy.getInt(PREF_DIFF_TYPE, TextDiffType.UNIFIED.value)

            return TextDiffType.fromValue(diffTypeValue)
        }
        set(placement) {
            preferencesLegacy.putInt(PREF_DIFF_TYPE, placement.value)

            _textDiffTypeFlow.value = textDiffType
        }

    var diffDisplayFullFile: Boolean
        get() {
            return preferencesLegacy.getBoolean(PREF_DIFF_FULL_FILE, false)
        }
        set(newValue) {
            preferencesLegacy.putBoolean(PREF_DIFF_TYPE, newValue)

            _textDiffFullFileFlow.value = newValue
        }

    fun saveCustomTheme(filePath: String) {
        val file = File(filePath)
        val content = file.readText()

        /*Json.decodeFromString<ColorsScheme>(content) // Load to see if it's valid (it will crash if not)

        preferences.put(PREF_CUSTOM_THEME, content)
        loadCustomTheme()*/
    }

    fun loadCustomTheme() {
        val themeJson = preferencesLegacy.get(PREF_CUSTOM_THEME, null)
        if (themeJson != null) {
            // _customThemeFlow.value = Json.decodeFromString<ColorsScheme>(themeJson)
        }
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