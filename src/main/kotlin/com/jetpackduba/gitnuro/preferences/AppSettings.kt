package com.jetpackduba.gitnuro.preferences

import com.jetpackduba.gitnuro.extensions.defaultWindowPlacement
import com.jetpackduba.gitnuro.system.OS
import com.jetpackduba.gitnuro.system.getCurrentOs
import com.jetpackduba.gitnuro.theme.ColorsScheme
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.viewmodels.TextDiffType
import com.jetpackduba.gitnuro.viewmodels.textDiffTypeFromValue
import kotlinx.coroutines.flow.*
import kotlinx.serialization.decodeFromString
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
private const val PREF_COMMITS_LIMIT = "commitsLimit"
private const val PREF_COMMITS_LIMIT_ENABLED = "commitsLimitEnabled"
private const val PREF_WINDOW_PLACEMENT = "windowsPlacement"
private const val PREF_CUSTOM_THEME = "customTheme"
private const val PREF_UI_SCALE = "ui_scale"
private const val PREF_DIFF_TYPE = "diffType"
private const val PREF_DIFF_FULL_FILE = "diffFullFile"
private const val PREF_SWAP_UNCOMMITED_CHANGES = "inverseUncommitedChanges"
private const val PREF_TERMINAL_PATH = "terminalPath"


private const val PREF_GIT_FF_MERGE = "gitFFMerge"
private const val PREF_GIT_PULL_REBASE = "gitPullRebase"
private const val PREF_GIT_PUSH_WITH_LEASE = "gitPushWithLease"

private const val DEFAULT_COMMITS_LIMIT = 1000
private const val DEFAULT_COMMITS_LIMIT_ENABLED = true
private const val DEFAULT_SWAP_UNCOMMITED_CHANGES = false
const val DEFAULT_UI_SCALE = -1f

@Singleton
class AppSettings @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    private val _themeState = MutableStateFlow(theme)
    val themeState = _themeState.asStateFlow()

    private val _commitsLimitEnabledFlow = MutableStateFlow(commitsLimitEnabled)
    val commitsLimitEnabledFlow = _commitsLimitEnabledFlow.asStateFlow()

    private val _swapUncommitedChangesFlow = MutableStateFlow(swapUncommitedChanges)
    val swapUncommitedChangesFlow = _swapUncommitedChangesFlow.asStateFlow()

    private val _ffMergeFlow = MutableStateFlow(ffMerge)
    val ffMergeFlow = _ffMergeFlow.asStateFlow()

    private val _pullRebaseFlow = MutableStateFlow(pullRebase)
    val pullRebaseFlow = _pullRebaseFlow.asStateFlow()

    private val _pushWithLeaseFlow = MutableStateFlow(pushWithLease)
    val pushWithLeaseFlow: StateFlow<Boolean> = _pushWithLeaseFlow.asStateFlow()

    private val _commitsLimitFlow = MutableSharedFlow<Int>()
    val commitsLimitFlow = _commitsLimitFlow.asSharedFlow()

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

    var latestTabsOpened: String
        get() = preferences.get(PREF_LATEST_REPOSITORIES_TABS_OPENED, "")
        set(value) {
            preferences.put(PREF_LATEST_REPOSITORIES_TABS_OPENED, value)
        }

    var latestRepositoryTabSelected: String
        get() = preferences.get(PREF_LATEST_REPOSITORY_TAB_SELECTED, "")
        set(value) {
            preferences.put(PREF_LATEST_REPOSITORY_TAB_SELECTED, value)
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

    var commitsLimitEnabled: Boolean
        get() {
            return preferences.getBoolean(PREF_COMMITS_LIMIT_ENABLED, DEFAULT_COMMITS_LIMIT_ENABLED)
        }
        set(value) {
            preferences.putBoolean(PREF_COMMITS_LIMIT_ENABLED, value)
            _commitsLimitEnabledFlow.value = value
        }

    var swapUncommitedChanges: Boolean
        get() {
            return preferences.getBoolean(PREF_SWAP_UNCOMMITED_CHANGES, DEFAULT_SWAP_UNCOMMITED_CHANGES)
        }
        set(value) {
            preferences.putBoolean(PREF_SWAP_UNCOMMITED_CHANGES, value)
            _swapUncommitedChangesFlow.value = value
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

    val commitsLimit: Int
        get() {
            return preferences.getInt(PREF_COMMITS_LIMIT, DEFAULT_COMMITS_LIMIT)
        }

    suspend fun setCommitsLimit(value: Int) {
        preferences.putInt(PREF_COMMITS_LIMIT, value)
        _commitsLimitFlow.emit(value)
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

// TODO migrate old prefs path to new one?
fun initPreferencesPath() {
    if (getCurrentOs() == OS.LINUX) {
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