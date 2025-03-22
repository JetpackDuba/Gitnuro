package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.Logging
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.managers.newErrorNow
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.theme.LinesHeightType
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.ui.dialogs.settings.ProxyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.awt.Desktop
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SettingsViewModel"

@Singleton
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val openFilePickerUseCase: OpenFilePickerUseCase,
    private val logging: Logging,
    @AppCoroutineScope private val appScope: CoroutineScope,
) {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1


    val themeState = appSettingsRepository.themeState
    val linesHeightTypeState = appSettingsRepository.linesHeightTypeState
    val ffMergeFlow = appSettingsRepository.ffMergeFlow
    val includeBranchNameFlow = appSettingsRepository.includeBranchNameFlow
    val pullRebaseFlow = appSettingsRepository.pullRebaseFlow
    val pushWithLeaseFlow = appSettingsRepository.pushWithLeaseFlow
    val swapUncommittedChangesFlow = appSettingsRepository.swapUncommittedChangesFlow
    val cacheCredentialsInMemoryFlow = appSettingsRepository.cacheCredentialsInMemoryFlow
    val verifySslFlow = appSettingsRepository.verifySslFlow
    val terminalPathFlow = appSettingsRepository.terminalPathFlow

    var scaleUi: Float
        get() = appSettingsRepository.scaleUi
        set(value) {
            appSettingsRepository.scaleUi = value
        }

    var swapUncommittedChanges: Boolean
        get() = appSettingsRepository.swapUncommittedChanges
        set(value) {
            appSettingsRepository.swapUncommittedChanges = value
        }

    var ffMerge: Boolean
        get() = appSettingsRepository.ffMerge
        set(value) {
            appSettingsRepository.ffMerge = value
        }

    var includeBranchName: Boolean
        get() = appSettingsRepository.includeBranchName
        set(value) {
            appSettingsRepository.includeBranchName = value
        }

    var cacheCredentialsInMemory: Boolean
        get() = appSettingsRepository.cacheCredentialsInMemory
        set(value) {
            appSettingsRepository.cacheCredentialsInMemory = value
        }

    var verifySsl: Boolean
        get() = appSettingsRepository.verifySsl
        set(value) {
            appSettingsRepository.verifySsl = value
        }

    var pullRebase: Boolean
        get() = appSettingsRepository.pullRebase
        set(value) {
            appSettingsRepository.pullRebase = value
        }

    var pushWithLease: Boolean
        get() = appSettingsRepository.pushWithLease
        set(value) {
            appSettingsRepository.pushWithLease = value
        }

    var theme: Theme
        get() = appSettingsRepository.theme
        set(value) {
            appSettingsRepository.theme = value
        }

    var linesHeightType: LinesHeightType
        get() = appSettingsRepository.linesHeightType
        set(value) {
            appSettingsRepository.linesHeightType = value
        }

    var terminalPath: String
        get() = appSettingsRepository.terminalPath
        set(value) {
            appSettingsRepository.terminalPath = value
        }

    var useProxy: Boolean
        get() = appSettingsRepository.useProxy
        set(value) {
            appSettingsRepository.useProxy = value
        }

    var proxyType: ProxyType
        get() = appSettingsRepository.proxyType
        set(value) {
            appSettingsRepository.proxyType = value
        }

    var proxyHostName: String
        get() = appSettingsRepository.proxyHostName
        set(value) {
            appSettingsRepository.proxyHostName = value
        }

    var proxyPortNumber: Int
        get() = appSettingsRepository.proxyPortNumber
        set(value) {
            appSettingsRepository.proxyPortNumber = value
        }

    var proxyUseAuth: Boolean
        get() = appSettingsRepository.proxyUseAuth
        set(value) {
            appSettingsRepository.proxyUseAuth = value
        }

    var proxyHostUser: String
        get() = appSettingsRepository.proxyHostUser
        set(value) {
            appSettingsRepository.proxyHostUser = value
        }

    var proxyHostPassword: String
        get() = appSettingsRepository.proxyHostPassword
        set(value) {
            appSettingsRepository.proxyHostPassword = value
        }

    fun saveCustomTheme(filePath: String): Error? {
        return try {
            appSettingsRepository.saveCustomTheme(filePath)
            null
        } catch (ex: Exception) {
            ex.printStackTrace()
            newErrorNow(
                TaskType.SAVE_CUSTOM_THEME,
                ex, // TODO Pass a proper exception with the commented strings
//                "Saving theme failed",
//                "Failed to parse selected theme JSON. Please check if it's valid and try again.",
            )
        }
    }

    fun openFileDialog(): String? {
        return openFilePickerUseCase(PickerType.FILES, null)
    }

    fun openLogsFolderInFileExplorer() {
        try {
            Desktop.getDesktop().open(logging.logsDirectory)
        } catch (ex: Exception) {
            printError(TAG, ex.message.orEmpty(), ex)
        }
    }
}