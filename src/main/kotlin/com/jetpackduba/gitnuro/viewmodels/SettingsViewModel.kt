package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.theme.Theme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsViewModel @Inject constructor(
    val appSettings: AppSettings,
) {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1

    val themeState = appSettings.themeState
    val customThemeFlow = appSettings.customThemeFlow
    val ffMergeFlow = appSettings.ffMergeFlow
    val commitsLimitEnabledFlow = appSettings.commitsLimitEnabledFlow

    var scaleUi: Float
        get() = appSettings.scaleUi
        set(value) {
            appSettings.scaleUi = value
        }

    var commitsLimitEnabled: Boolean
        get() = appSettings.commitsLimitEnabled
        set(value) {
            appSettings.commitsLimitEnabled = value
        }

    var ffMerge: Boolean
        get() = appSettings.ffMerge
        set(value) {
            appSettings.ffMerge = value
        }

    var theme: Theme
        get() = appSettings.theme
        set(value) {
            appSettings.theme = value
        }

    fun saveCustomTheme(filePath: String) {
        appSettings.saveCustomTheme(filePath)
    }


    fun resetInfo() {
        commitsLimit = appSettings.commitsLimit
    }

    fun savePendingChanges() {
        val commitsLimit = this.commitsLimit

        if (appSettings.commitsLimit != commitsLimit) {
            appSettings.commitsLimit = commitsLimit
        }
    }
}