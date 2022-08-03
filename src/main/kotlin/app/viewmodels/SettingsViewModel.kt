package app.viewmodels

import app.preferences.AppPreferences
import app.theme.Theme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsViewModel @Inject constructor(
    val appPreferences: AppPreferences,
) {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1

    val themeState = appPreferences.themeState
    val customThemeFlow = appPreferences.customThemeFlow
    val ffMergeFlow = appPreferences.ffMergeFlow
    val commitsLimitEnabledFlow = appPreferences.commitsLimitEnabledFlow

    var scaleUi: Float
        get() = appPreferences.scaleUi
        set(value) {
            appPreferences.scaleUi = value
        }

    var commitsLimitEnabled: Boolean
        get() = appPreferences.commitsLimitEnabled
        set(value) {
            appPreferences.commitsLimitEnabled = value
        }

    var ffMerge: Boolean
        get() = appPreferences.ffMerge
        set(value) {
            appPreferences.ffMerge = value
        }

    var theme: Theme
        get() = appPreferences.theme
        set(value) {
            appPreferences.theme = value
        }

    fun saveCustomTheme(filePath: String) {
        appPreferences.saveCustomTheme(filePath)
    }


    fun resetInfo() {
        commitsLimit = appPreferences.commitsLimit
    }

    fun savePendingChanges() {
        val commitsLimit = this.commitsLimit

        if (appPreferences.commitsLimit != commitsLimit) {
            appPreferences.commitsLimit = commitsLimit
        }
    }
}