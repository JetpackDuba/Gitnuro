package app

import java.util.prefs.Preferences
import javax.inject.Inject

private const val PREFERENCES_NAME = "GitnuroConfig"

private const val PREF_LAST_OPENED_REPOSITORY_PATH = "lastOpenedRepositoryPath"

class GPreferences @Inject constructor() {
    private val preferences: Preferences = Preferences.userRoot().node(PREFERENCES_NAME)

    var latestOpenedRepositoryPath: String
        get() = preferences.get(PREF_LAST_OPENED_REPOSITORY_PATH, "")
        set(value) {
            preferences.put(PREF_LAST_OPENED_REPOSITORY_PATH, value)
        }
}