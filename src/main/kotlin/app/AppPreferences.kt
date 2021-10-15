package app

import java.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LATEST_REPOSITORIES_OPENED = "latestRepositoriesOpened"
private const val PREF_LAST_OPENED_REPOSITORY_PATH = "lastOpenedRepositoryPath"

@Singleton
class AppPreferences @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    var latestTabsOpened: String
        get() = preferences.get(PREF_LATEST_REPOSITORIES_OPENED, "")
        set(value) {
            preferences.put(PREF_LATEST_REPOSITORIES_OPENED, value)
        }

    var latestOpenedRepositoryPath: String
        get() = preferences.get(PREF_LAST_OPENED_REPOSITORY_PATH, "")
        set(value) {
            preferences.put(PREF_LAST_OPENED_REPOSITORY_PATH, value)
        }
}