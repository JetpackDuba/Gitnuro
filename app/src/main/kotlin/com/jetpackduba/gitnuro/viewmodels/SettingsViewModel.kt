package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.runtime.Stable
import com.jetpackduba.gitnuro.LogsRepository
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.domain.models.AppConfig
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.Error
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.newErrorNow
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.PickerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SettingsViewModel"

@Singleton
class SettingsViewModel @Inject constructor(
    private val appSettingsService: AppSettingsService,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val logsRepository: LogsRepository,
    @param:AppCoroutineScope private val appScope: CoroutineScope,
) : TabViewModel() {
    val settingsViewState = settingsState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = emptySettingsState()
        )

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SetConfig -> setAppConfiguration(action.configuration)
            SettingsAction.OpenLogsFolder -> TODO() // TODO implement this
        }
    }

    private fun setAppConfiguration(appConfig: AppConfig) = appScope.launch {
        appSettingsService.setConfiguration(appConfig)
    }

    fun saveCustomTheme(filePath: String): Error? {
        return try {
            //appSettingsService.saveCustomTheme(filePath)
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
        return openFilePickerGitAction(PickerType.FILES, null)
    }

    fun openLogsFolderInFileExplorer() {
        try {
            Desktop.getDesktop().open(logsRepository.logsDirectory)
        } catch (ex: Exception) {
            printError(TAG, ex.message.orEmpty(), ex)
        }
    }

    fun isValidDateFormat(value: String): Boolean {
        return try {
            val zoneId = ZoneId.systemDefault()
            val sdf = DateTimeFormatter.ofPattern(value)
            sdf.format(Instant.now().atZone(zoneId).toLocalDate())
            true
        } catch (ex: Exception) {
            printError(TAG, "Is valid format date: ${ex.message}", ex)
            false
        }
    }

    private fun settingsState(): Flow<SettingsViewState> {
        return combine(
            appSettingsService.scaleUi,
            appSettingsService.theme,
            appSettingsService.customTheme,
            appSettingsService.linesHeightType,
            appSettingsService.dateFormatUseDefault,
            appSettingsService.dateFormatCustomFormat,
            appSettingsService.dateFormatIs24h,
            appSettingsService.dateFormatUseRelative,
            appSettingsService.avatarProvider,
            appSettingsService.swapStatusPanes,
            appSettingsService.pullWithRebase,
            appSettingsService.pushWithLease,
            appSettingsService.fastForwardMerge,
            appSettingsService.autoStashOnMerge,
            appSettingsService.cloneDefaultDirectory,
            appSettingsService.useProxy,
            appSettingsService.proxyUseAuth,
            appSettingsService.proxyType,
            appSettingsService.proxyHostName,
            appSettingsService.proxyPortNumber,
            appSettingsService.proxyHostUser,
            appSettingsService.proxyHostPassword,
            appSettingsService.verifySsl,
            appSettingsService.cacheCredentialsInMemory,
            appSettingsService.terminalPath,
        ) { scaleUi,
            theme,
            customTheme,
            linesHeightType,
            dateFormatUseDefault,
            dateFormatCustomFormat,
            dateFormatIs24h,
            dateFormatUseRelative,
            avatarProvider,
            swapStatusPanes,
            pullWithRebase,
            pushWithLease,
            fastForwardMerge,
            autoStashOnMerge,
            cloneDefaultDirectory,
            useProxy,
            proxyUseAuth,
            proxyType,
            proxyHostName,
            proxyPortNumber,
            proxyHostUser,
            proxyHostPassword,
            verifySsl,
            cacheCredentialsInMemory,
            terminalPath ->

            SettingsViewState(
                scaleUi,
                theme,
                customTheme,
                linesHeightType,
                dateFormatUseDefault,
                dateFormatCustomFormat,
                dateFormatIs24h,
                dateFormatUseRelative,
                avatarProvider,
                swapStatusPanes,
                pullWithRebase,
                pushWithLease,
                fastForwardMerge,
                autoStashOnMerge,
                cloneDefaultDirectory,
                useProxy,
                proxyUseAuth,
                proxyType,
                proxyHostName,
                proxyPortNumber,
                proxyHostUser,
                proxyHostPassword,
                verifySsl,
                cacheCredentialsInMemory,
                terminalPath,
            )
        }
    }

    private fun emptySettingsState(): SettingsViewState {
        return SettingsViewState(
            scaleUi = null,
            theme = Theme.Light,
            customTheme = "",
            linesHeightType = LinesHeightType.SPACED,
            dateFormatUseDefault = false,
            dateFormatCustomFormat = "",
            dateFormatIs24h = false,
            dateFormatUseRelative = false,
            avatarProvider = AvatarProviderType.Gravatar,
            swapStatusPanes = false,
            pullWithRebase = false,
            pushWithLease = false,
            fastForwardMerge = false,
            autoStashOnMerge = false,
            cloneDefaultDirectory = "",
            useProxy = false,
            proxyUseAuth = false,
            proxyType = ProxyType.HTTP,
            proxyHostName = "",
            proxyPortNumber = null,
            proxyHostUser = "",
            proxyHostPassword = "",
            verifySsl = false,
            cacheCredentialsInMemory = false,
            terminalPath = "",
        )
    }
}

@Stable
data class SettingsViewState(
    val scaleUi: Float?,
    val theme: Theme,
    val customTheme: String?,
    val linesHeightType: LinesHeightType,
    val dateFormatUseDefault: Boolean,
    val dateFormatCustomFormat: String,
    val dateFormatIs24h: Boolean,
    val dateFormatUseRelative: Boolean,
    val avatarProvider: AvatarProviderType,
    val swapStatusPanes: Boolean,
    val pullWithRebase: Boolean,
    val pushWithLease: Boolean,
    val fastForwardMerge: Boolean,
    val autoStashOnMerge: Boolean,
    val cloneDefaultDirectory: String?,
    val useProxy: Boolean,
    val proxyUseAuth: Boolean,
    val proxyType: ProxyType,
    val proxyHostName: String?,
    val proxyPortNumber: Int?,
    val proxyHostUser: String?,
    val proxyHostPassword: String?,
    val verifySsl: Boolean,
    val cacheCredentialsInMemory: Boolean,
    val terminalPath: String?,
)

sealed interface SettingsAction {
    data class SetConfig(val configuration: AppConfig): SettingsAction
    data object OpenLogsFolder: SettingsAction
}