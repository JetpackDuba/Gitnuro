package app

import app.theme.Themes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_TABS_OPENED = "latestRepositoriesTabsOpened"
private const val PREF_LAST_OPENED_REPOSITORIES_PATH = "lastOpenedRepositoriesList"
private const val PREF_THEME = "theme"
private const val PREF_COMMITS_LIMIT = "commitsLimit"
private const val PREF_COMMITS_LIMIT_ENABLED = "commitsLimitEnabled"

private const val DEFAULT_COMMITS_LIMIT = 1000
private const val DEFAULT_COMMITS_LIMIT_ENABLED = true

@Singleton
class AppPreferences @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    private val _themeState = MutableStateFlow(theme)
    val themeState: StateFlow<Themes> = _themeState

    private val _commitsLimitEnabledFlow = MutableStateFlow(true)
    val commitsLimitEnabledFlow: StateFlow<Boolean> = _commitsLimitEnabledFlow

    private val _commitsLimitFlow = MutableStateFlow(commitsLimit)
    val commitsLimitFlow: StateFlow<Int> = _commitsLimitFlow

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

    var theme: Themes
        get() {
            val lastTheme = preferences.get(PREF_THEME, Themes.DARK.toString())
            return Themes.valueOf(lastTheme)
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

    var commitsLimit: Int
        get() {
            return preferences.getInt(PREF_COMMITS_LIMIT, DEFAULT_COMMITS_LIMIT)
        }
        set(value) {
            preferences.putInt(PREF_COMMITS_LIMIT, value)
            _commitsLimitFlow.value = value
        }
}