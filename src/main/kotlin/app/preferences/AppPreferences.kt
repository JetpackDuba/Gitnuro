package app.preferences

import app.extensions.defaultWindowPlacement
import app.theme.ColorsScheme
import app.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_TABS_OPENED = "latestRepositoriesTabsOpened"
private const val PREF_LAST_OPENED_REPOSITORIES_PATH = "lastOpenedRepositoriesList"
private const val PREF_THEME = "theme"
private const val PREF_COMMITS_LIMIT = "commitsLimit"
private const val PREF_COMMITS_LIMIT_ENABLED = "commitsLimitEnabled"
private const val PREF_WINDOW_PLACEMENT = "windowsPlacement"
private const val PREF_CUSTOM_THEME = "customTheme"
private const val PREF_UI_SCALE = "ui_scale"


private const val PREF_GIT_FF_MERGE = "gitFFMerge"

private const val DEFAULT_COMMITS_LIMIT = 1000
private const val DEFAULT_COMMITS_LIMIT_ENABLED = true
const val DEFAULT_UI_SCALE = -1f

@Singleton
class AppPreferences @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    private val _themeState = MutableStateFlow(theme)
    val themeState: StateFlow<Theme> = _themeState

    private val _commitsLimitEnabledFlow = MutableStateFlow(commitsLimitEnabled)
    val commitsLimitEnabledFlow: StateFlow<Boolean> = _commitsLimitEnabledFlow

    private val _ffMergeFlow = MutableStateFlow(ffMerge)
    val ffMergeFlow: StateFlow<Boolean> = _ffMergeFlow

    private val _commitsLimitFlow = MutableStateFlow(commitsLimit)
    val commitsLimitFlow: StateFlow<Int> = _commitsLimitFlow

    private val _customThemeFlow = MutableStateFlow<ColorsScheme?>(null)
    val customThemeFlow: StateFlow<ColorsScheme?> = _customThemeFlow

    private val _scaleUiFlow = MutableStateFlow(scaleUi)
    val scaleUiFlow: StateFlow<Float> = _scaleUiFlow

    var latestTabsOpened: String
        get() = preferences.get(PREF_LATEST_REPOSITORIES_TABS_OPENED, "")
        set(value) {
            preferences.put(PREF_LATEST_REPOSITORIES_TABS_OPENED, value)
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

    var commitsLimit: Int
        get() {
            return preferences.getInt(PREF_COMMITS_LIMIT, DEFAULT_COMMITS_LIMIT)
        }
        set(value) {
            preferences.putInt(PREF_COMMITS_LIMIT, value)
            _commitsLimitFlow.value = value
        }

    var windowPlacement: WindowsPlacementPreference
        get() {
            val placement = preferences.getInt(PREF_WINDOW_PLACEMENT, defaultWindowPlacement.value)

            return WindowsPlacementPreference(placement)
        }
        set(placement) {
            preferences.putInt(PREF_WINDOW_PLACEMENT, placement.value)
        }

    fun saveCustomTheme(filePath: String) {
        try {
            val file = File(filePath)
            val content = file.readText()

            Json.decodeFromString<ColorsScheme>(content) // Load to see if it's valid (it will crash if not)

            preferences.put(PREF_CUSTOM_THEME, content)
            loadCustomTheme()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun loadCustomTheme() {
        val themeJson = preferences.get(PREF_CUSTOM_THEME, null)
        if (themeJson != null) {
            _customThemeFlow.value = Json.decodeFromString<ColorsScheme>(themeJson)
        }
    }
}