package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.theme.Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    @AppCoroutineScope private val appScope: CoroutineScope,
) {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1

    val themeState = appSettings.themeState
    val ffMergeFlow = appSettings.ffMergeFlow
    val pullRebaseFlow = appSettings.pullRebaseFlow
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

    var pullRebase: Boolean
        get() = appSettings.pullRebase
        set(value) {
            appSettings.pullRebase = value
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

    fun savePendingChanges() = appScope.launch {
        val commitsLimit = this@SettingsViewModel.commitsLimit

        if (appSettings.commitsLimit != commitsLimit) {
            appSettings.setCommitsLimit(commitsLimit)
        }
    }
}