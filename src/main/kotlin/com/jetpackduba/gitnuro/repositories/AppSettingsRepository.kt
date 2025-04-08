package com.jetpackduba.gitnuro.repositories

import com.jetpackduba.gitnuro.SettingsDefaults
import com.jetpackduba.gitnuro.extensions.defaultWindowPlacement
import com.jetpackduba.gitnuro.models.DateTimeFormat
import com.jetpackduba.gitnuro.preferences.AvatarProviderType
import com.jetpackduba.gitnuro.preferences.WindowsPlacementPreference
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.currentOs
import com.jetpackduba.gitnuro.theme.ColorsScheme
import com.jetpackduba.gitnuro.theme.LinesHeightType
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.ui.dialogs.settings.ProxyType
import com.jetpackduba.gitnuro.viewmodels.TextDiffType
import com.jetpackduba.gitnuro.viewmodels.textDiffTypeFromValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_TABS_OPENED = "latestRepositoriesTabsOpened"
private const val PREF_LATEST_REPOSITORY_TAB_SELECTED = "latestRepositoryTabSelected"
private const val PREF_LAST_OPENED_REPOSITORIES_PATH = "lastOpenedRepositoriesList"
private const val PREF_THEME = "theme"
private const val PREF_LINE_HEIGHT_TYPE = "linesHeight"
private const val PREF_WINDOW_PLACEMENT = "windowsPlacement"
private const val PREF_CUSTOM_THEME = "customTheme"
private const val PREF_UI_SCALE = "ui_scale"
private const val PREF_DIFF_TYPE = "diffType"
private const val PREF_DIFF_FULL_FILE = "diffFullFile"
private const val PREF_SWAP_UNCOMMITTED_CHANGES = "inverseUncommittedChanges"
private const val PREF_TERMINAL_PATH = "terminalPath"
private const val PREF_SHOW_CHANGES_AS_TREE = "showChangesAsTree"
private const val PREF_USE_PROXY = "useProxy"
private const val PREF_AVATAR_PROVIDER = "avatarProvider"
private const val PREF_DATE_FORMAT_USE_DEFAULT = "dateFormatUseSystemDefault"
private const val PREF_DATE_FORMAT_CUSTOM_FORMAT = "dateFormatCustomFormat"
private const val PREF_DATE_FORMAT_IS_24H = "dateFormatIs24h"
private const val PREF_DATE_FORMAT_USE_RELATIVE = "dateFormatUseRelative"
private const val PREF_PROXY_TYPE = "proxyType"
private const val PREF_PROXY_HOST_NAME = "proxyHostName"
private const val PREF_PROXY_PORT = "proxyPort"
private const val PREF_PROXY_USE_AUTH = "proxyUseAuth"
private const val PREF_PROXY_USER = "proxyHostUser"
private const val PREF_PROXY_PASSWORD = "proxyHostPassword"
private const val PREF_CACHE_CREDENTIALS_IN_MEMORY = "credentialsInMemory"
private const val PREF_FIRST_PANE_WIDTH = "firstPaneWidth"
private const val PREF_THIRD_PANE_WIDTH = "thirdPaneWidth"

private const val PREF_GIT_FF_MERGE = "gitFFMerge"
private const val PREF_GIT_PULL_REBASE = "gitPullRebase"
private const val PREF_GIT_PUSH_WITH_LEASE = "gitPushWithLease"

private const val PREF_VERIFY_SSL = "verifySsl"

private const val DEFAULT_SWAP_UNCOMMITTED_CHANGES = false
private const val DEFAULT_SHOW_CHANGES_AS_TREE = false
private const val DEFAULT_CACHE_CREDENTIALS_IN_MEMORY = true
private const val DEFAULT_VERIFY_SSL = true
private const val DEFAULT_FIRST_PANE_WIDTH = 220f
private const val DEFAULT_THIRD_PANE_WIDTH = 330f
const val DEFAULT_UI_SCALE = -1f

@Singleton
class AppSettingsRepository @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    private val _themeState = MutableStateFlow(theme)
    val themeState = _themeState.asStateFlow()

    private val _linesHeightTypeState = MutableStateFlow(linesHeightType)
    val linesHeightTypeState = _linesHeightTypeState.asStateFlow()

    private val _swapUncommittedChangesFlow = MutableStateFlow(swapUncommittedChanges)
    val swapUncommittedChangesFlow = _swapUncommittedChangesFlow.asStateFlow()

    private val _showChangesAsTreeFlow = MutableStateFlow(showChangesAsTree)
    val showChangesAsTreeFlow = _showChangesAsTreeFlow.asStateFlow()

    private val _cacheCredentialsInMemoryFlow = MutableStateFlow(cacheCredentialsInMemory)
    val cacheCredentialsInMemoryFlow = _cacheCredentialsInMemoryFlow.asStateFlow()

    private val _verifySslFlow = MutableStateFlow(cacheCredentialsInMemory)
    val verifySslFlow = _verifySslFlow.asStateFlow()

    private val _ffMergeFlow = MutableStateFlow(ffMerge)
    val ffMergeFlow = _ffMergeFlow.asStateFlow()

    private val _pullRebaseFlow = MutableStateFlow(pullRebase)
    val pullRebaseFlow = _pullRebaseFlow.asStateFlow()

    private val _pushWithLeaseFlow = MutableStateFlow(pushWithLease)
    val pushWithLeaseFlow: StateFlow<Boolean> = _pushWithLeaseFlow.asStateFlow()

    private val _customThemeFlow = MutableStateFlow<ColorsScheme?>(null)
    val customThemeFlow = _customThemeFlow.asStateFlow()

    private val _scaleUiFlow = MutableStateFlow(scaleUi)
    val scaleUiFlow = _scaleUiFlow.asStateFlow()

    private val _textDiffTypeFlow = MutableStateFlow(textDiffType)
    val textDiffTypeFlow = _textDiffTypeFlow.asStateFlow()

    private val _textDiffFullFileFlow = MutableStateFlow(diffDisplayFullFile)
    val diffDisplayFullFileFlow = _textDiffFullFileFlow.asStateFlow()

    private val _terminalPathFlow = MutableStateFlow(terminalPath)
    val terminalPathFlow = _terminalPathFlow.asStateFlow()

    private val _avatarProviderFlow = MutableStateFlow(avatarProviderType)
    val avatarProviderTypeFlow = _avatarProviderFlow.asStateFlow()

    private val _dateTimeFormatFlow = MutableStateFlow(dateTimeFormat)
    val dateTimeFormatFlow = _dateTimeFormatFlow.asStateFlow()

    private val _proxyFlow = MutableStateFlow(
        ProxySettings(
            useProxy,
            proxyType,
            proxyHostName,
            proxyPortNumber,
            proxyUseAuth,
            proxyHostUser,
            proxyHostPassword,
        )
    )

    val proxyFlow = _proxyFlow.asStateFlow()

    var latestTabsOpened: String
        get() = preferences.get(PREF_LATEST_REPOSITORIES_TABS_OPENED, "")
        set(value) {
            preferences.put(PREF_LATEST_REPOSITORIES_TABS_OPENED, value)
        }

    var latestRepositoryTabSelected: Int
        get() = preferences.getInt(PREF_LATEST_REPOSITORY_TAB_SELECTED, -1)
        set(value) {
            preferences.putInt(PREF_LATEST_REPOSITORY_TAB_SELECTED, value)
        }

    var latestOpenedRepositoriesPath: String
        get() = preferences.get(PREF_LAST_OPENED_REPOSITORIES_PATH, "")
        set(value) {
            preferences.put(PREF_LAST_OPENED_REPOSITORIES_PATH, value)
        }

    var theme: Theme
        get() {
            val lastTheme = preferences.get(PREF_THEME, Theme.DARK.toString())
            return try {
                Theme.valueOf(lastTheme)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Theme.DARK
            }
        }
        set(value) {
            preferences.put(PREF_THEME, value.toString())
            _themeState.value = value
        }

    var linesHeightType: LinesHeightType
        get() {
            val lineHeightTypeValue = preferences.getInt(PREF_LINE_HEIGHT_TYPE, LinesHeightType.SPACED.value)
            return try {
                LinesHeightType.fromInt(lineHeightTypeValue)
            } catch (ex: Exception) {
                ex.printStackTrace()
                LinesHeightType.SPACED
            }
        }
        set(value) {
            preferences.putInt(PREF_LINE_HEIGHT_TYPE, value.value)
            _linesHeightTypeState.value = value
        }

    var swapUncommittedChanges: Boolean
        get() {
            return preferences.getBoolean(PREF_SWAP_UNCOMMITTED_CHANGES, DEFAULT_SWAP_UNCOMMITTED_CHANGES)
        }
        set(value) {
            preferences.putBoolean(PREF_SWAP_UNCOMMITTED_CHANGES, value)
            _swapUncommittedChangesFlow.value = value
        }

    var showChangesAsTree: Boolean
        get() {
            return preferences.getBoolean(PREF_SHOW_CHANGES_AS_TREE, DEFAULT_SHOW_CHANGES_AS_TREE)
        }
        set(value) {
            preferences.putBoolean(PREF_SHOW_CHANGES_AS_TREE, value)
            _showChangesAsTreeFlow.value = value
        }

    var cacheCredentialsInMemory: Boolean
        get() {
            return preferences.getBoolean(PREF_CACHE_CREDENTIALS_IN_MEMORY, DEFAULT_CACHE_CREDENTIALS_IN_MEMORY)
        }
        set(value) {
            preferences.putBoolean(PREF_CACHE_CREDENTIALS_IN_MEMORY, value)
            _cacheCredentialsInMemoryFlow.value = value
        }

    var firstPaneWidth: Float
        get() {
            return preferences.getFloat(PREF_FIRST_PANE_WIDTH, DEFAULT_FIRST_PANE_WIDTH)
        }
        set(value) {
            preferences.putFloat(PREF_FIRST_PANE_WIDTH, value)
        }

    var thirdPaneWidth: Float
        get() {
            return preferences.getFloat(PREF_THIRD_PANE_WIDTH, DEFAULT_THIRD_PANE_WIDTH)
        }
        set(value) {
            preferences.putFloat(PREF_THIRD_PANE_WIDTH, value)
        }

    var verifySsl: Boolean
        get() {
            return preferences.getBoolean(PREF_VERIFY_SSL, DEFAULT_VERIFY_SSL)
        }
        set(value) {
            preferences.putBoolean(PREF_VERIFY_SSL, value)
            _verifySslFlow.value = value
        }

    var scaleUi: Float
        get() {
            return preferences.getFloat(PREF_UI_SCALE, DEFAULT_UI_SCALE)
        }
        set(value) {
            preferences.putFloat(PREF_UI_SCALE, value)
            _scaleUiFlow.value = value
        }

    /**
     * Property that decides if the merge should fast-forward when possible
     */
    var ffMerge: Boolean
        get() {
            return preferences.getBoolean(PREF_GIT_FF_MERGE, true)
        }
        set(value) {
            preferences.putBoolean(PREF_GIT_FF_MERGE, value)
            _ffMergeFlow.value = value
        }

    /**
     * Property that decides if the merge should fast-forward when possible
     */
    var pullRebase: Boolean
        get() {
            return preferences.getBoolean(PREF_GIT_PULL_REBASE, false)
        }
        set(value) {
            preferences.putBoolean(PREF_GIT_PULL_REBASE, value)
            _pullRebaseFlow.value = value
        }

    var pushWithLease: Boolean
        get() {
            return preferences.getBoolean(PREF_GIT_PUSH_WITH_LEASE, true)
        }
        set(value) {
            preferences.putBoolean(PREF_GIT_PUSH_WITH_LEASE, value)
            _pushWithLeaseFlow.value = value
        }

    var windowPlacement: WindowsPlacementPreference
        get() {
            val placement = preferences.getInt(PREF_WINDOW_PLACEMENT, defaultWindowPlacement.value)

            return WindowsPlacementPreference(placement)
        }
        set(placement) {
            preferences.putInt(PREF_WINDOW_PLACEMENT, placement.value)
        }

    var textDiffType: TextDiffType
        get() {
            val diffTypeValue = preferences.getInt(PREF_DIFF_TYPE, TextDiffType.UNIFIED.value)

            return textDiffTypeFromValue(diffTypeValue)
        }
        set(placement) {
            preferences.putInt(PREF_DIFF_TYPE, placement.value)

            _textDiffTypeFlow.value = textDiffType
        }

    var diffDisplayFullFile: Boolean
        get() {
            return preferences.getBoolean(PREF_DIFF_FULL_FILE, false)
        }
        set(newValue) {
            preferences.putBoolean(PREF_DIFF_TYPE, newValue)

            _textDiffFullFileFlow.value = newValue
        }

    var terminalPath: String
        get() = preferences.get(PREF_TERMINAL_PATH, "")
        set(value) {
            preferences.put(PREF_TERMINAL_PATH, value)

            _terminalPathFlow.value = value
        }

    var avatarProviderType: AvatarProviderType
        get() = AvatarProviderType.getFromValue(
            preferences.getInt(
                PREF_AVATAR_PROVIDER,
                SettingsDefaults.defaultAvatarProviderType.value,
            )
        )
        set(newValue) {
            preferences.putInt(PREF_AVATAR_PROVIDER, newValue.value)
            _avatarProviderFlow.value = newValue
        }

    var dateTimeFormat: DateTimeFormat
        get() {
            val default = SettingsDefaults.defaultDateTimeFormat

            val useSystemDefault = preferences.getBoolean(PREF_DATE_FORMAT_USE_DEFAULT, default.useSystemDefault)
            val customFormat = preferences.get(PREF_DATE_FORMAT_CUSTOM_FORMAT, default.customFormat)
            val is24h = preferences.getBoolean(PREF_DATE_FORMAT_IS_24H, default.is24hours)
            val useRelativeDate = preferences.getBoolean(PREF_DATE_FORMAT_USE_RELATIVE, default.useRelativeDate)

            return DateTimeFormat(useSystemDefault, customFormat, is24h, useRelativeDate)
        }
        set(newValue) {
            preferences.putBoolean(PREF_DATE_FORMAT_USE_DEFAULT, newValue.useSystemDefault)
            preferences.put(PREF_DATE_FORMAT_CUSTOM_FORMAT, newValue.customFormat)
            preferences.putBoolean(PREF_DATE_FORMAT_IS_24H, newValue.is24hours)
            preferences.putBoolean(PREF_DATE_FORMAT_USE_RELATIVE, newValue.useRelativeDate)

            _dateTimeFormatFlow.value = newValue
        }

    var useProxy: Boolean
        get() {
            return preferences.getBoolean(PREF_USE_PROXY, false)
        }
        set(value) {
            preferences.putBoolean(PREF_USE_PROXY, value)
            _proxyFlow.value = _proxyFlow.value.copy(useProxy = value)
        }

    var proxyUseAuth: Boolean
        get() {
            return preferences.getBoolean(PREF_PROXY_USE_AUTH, false)
        }
        set(value) {
            preferences.putBoolean(PREF_PROXY_USE_AUTH, value)
            _proxyFlow.value = _proxyFlow.value.copy(useAuth = value)
        }

    var proxyType: ProxyType
        get() {
            val value = preferences.getInt(PREF_PROXY_TYPE, ProxyType.HTTP.value)
            return ProxyType.fromInt(value)
        }
        set(value) {
            preferences.putInt(PREF_PROXY_TYPE, value.value)
            _proxyFlow.value = _proxyFlow.value.copy(proxyType = value)
        }

    var proxyHostName: String
        get() = preferences.get(PREF_PROXY_HOST_NAME, "")
        set(value) {
            preferences.put(PREF_PROXY_HOST_NAME, value)
            _proxyFlow.value = _proxyFlow.value.copy(hostName = value)
        }

    var proxyPortNumber: Int
        get() = preferences.getInt(PREF_PROXY_PORT, 80)
        set(value) {
            preferences.putInt(PREF_PROXY_PORT, value)
            _proxyFlow.value = _proxyFlow.value.copy(hostPort = value)
        }

    var proxyHostUser: String
        get() = preferences.get(PREF_PROXY_USER, "")
        set(value) {
            preferences.put(PREF_PROXY_USER, value)
            _proxyFlow.value = _proxyFlow.value.copy(hostUser = value)
        }

    var proxyHostPassword: String
        get() = preferences.get(PREF_PROXY_PASSWORD, "")
        set(value) {
            preferences.put(PREF_PROXY_PASSWORD, value)
            _proxyFlow.value = _proxyFlow.value.copy(hostPassword = value)
        }

    fun saveCustomTheme(filePath: String) {
        val file = File(filePath)
        val content = file.readText()

        Json.decodeFromString<ColorsScheme>(content) // Load to see if it's valid (it will crash if not)

        preferences.put(PREF_CUSTOM_THEME, content)
        loadCustomTheme()
    }

    fun loadCustomTheme() {
        val themeJson = preferences.get(PREF_CUSTOM_THEME, null)
        if (themeJson != null) {
            _customThemeFlow.value = Json.decodeFromString<ColorsScheme>(themeJson)
        }
    }
}

data class ProxySettings(
    val useProxy: Boolean,
    val proxyType: ProxyType,
    val hostName: String,
    val hostPort: Int,
    val useAuth: Boolean,
    val hostUser: String,
    val hostPassword: String,
)

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