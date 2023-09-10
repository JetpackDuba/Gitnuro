package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.managers.newErrorNow
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.ui.dialogs.settings.ProxyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsViewModel @Inject constructor(
    private val appSettings: AppSettings,
    private val openFilePickerUseCase: OpenFilePickerUseCase,
    @AppCoroutineScope private val appScope: CoroutineScope,
) {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1

    val themeState = appSettings.themeState
    val ffMergeFlow = appSettings.ffMergeFlow
    val pullRebaseFlow = appSettings.pullRebaseFlow
    val pushWithLeaseFlow = appSettings.pushWithLeaseFlow
    val commitsLimitEnabledFlow = appSettings.commitsLimitEnabledFlow
    val swapUncommittedChangesFlow = appSettings.swapUncommittedChangesFlow
    val cacheCredentialsInMemoryFlow = appSettings.cacheCredentialsInMemoryFlow
    val terminalPathFlow = appSettings.terminalPathFlow

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

    var swapUncommitedChanges: Boolean
        get() = appSettings.swapUncommittedChanges
        set(value) {
            appSettings.swapUncommittedChanges = value
        }

    var ffMerge: Boolean
        get() = appSettings.ffMerge
        set(value) {
            appSettings.ffMerge = value
        }

    var cacheCredentialsInMemory: Boolean
        get() = appSettings.cacheCredentialsInMemory
        set(value) {
            appSettings.cacheCredentialsInMemory = value
        }

    var pullRebase: Boolean
        get() = appSettings.pullRebase
        set(value) {
            appSettings.pullRebase = value
        }

    var pushWithLease: Boolean
        get() = appSettings.pushWithLease
        set(value) {
            appSettings.pushWithLease = value
        }

    var theme: Theme
        get() = appSettings.theme
        set(value) {
            appSettings.theme = value
        }

    var terminalPath: String
        get() = appSettings.terminalPath
        set(value) {
            appSettings.terminalPath = value
        }

    var useProxy: Boolean
        get() = appSettings.useProxy
        set(value) {
            appSettings.useProxy = value
        }

    var proxyType: ProxyType
        get() = appSettings.proxyType
        set(value) {
            appSettings.proxyType = value
        }

    var proxyHostName: String
        get() = appSettings.proxyHostName
        set(value) {
            appSettings.proxyHostName = value
        }

    var proxyPortNumber: Int
        get() = appSettings.proxyPortNumber
        set(value) {
            appSettings.proxyPortNumber = value
        }

    var proxyUseAuth: Boolean
        get() = appSettings.proxyUseAuth
        set(value) {
            appSettings.proxyUseAuth = value
        }

    var proxyHostUser: String
        get() = appSettings.proxyHostUser
        set(value) {
            appSettings.proxyHostUser = value
        }

    var proxyHostPassword: String
        get() = appSettings.proxyHostPassword
        set(value) {
            appSettings.proxyHostPassword = value
        }

    fun saveCustomTheme(filePath: String): Error? {
        return try {
            appSettings.saveCustomTheme(filePath)
            null
        } catch (ex: Exception) {
            ex.printStackTrace()
            newErrorNow(ex, "Saving theme failed", "Failed to parse selected theme JSON. Please check if it's valid and try again.")
        }
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

    fun openFileDialog(): String? {
        return openFilePickerUseCase(PickerType.FILES, null)
    }
}