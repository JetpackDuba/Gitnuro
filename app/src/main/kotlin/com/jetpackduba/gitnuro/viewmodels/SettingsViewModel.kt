package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.runtime.Stable
import com.jetpackduba.gitnuro.LogsRepository
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.data.repositories.configuration.AppSettingsRepository
import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.domain.models.AppConfiguration
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.Error
import com.jetpackduba.gitnuro.domain.models.ProxySettings
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.newErrorNow
import com.jetpackduba.gitnuro.domain.models.ui.LinesHeightType
import com.jetpackduba.gitnuro.domain.models.ui.Theme
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.PickerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
    private val appSettingsRepository: AppSettingsRepository,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val logsRepository: LogsRepository,
    private val getConfigurationUseCase: GetConfigurationUseCase,
    @param:AppCoroutineScope private val appScope: CoroutineScope,
) : TabViewModel() {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1


    val themeState = appSettingsRepository.theme
    val linesHeightTypeState = appSettingsRepository.linesHeightType
    var defaultCloneDirFlow = appSettingsRepository.cloneDefaultDirectory
    val ffMergeFlow = appSettingsRepository.fastForwardMerge
    val mergeAutoStashFlow = appSettingsRepository.autoStashOnMerge
    val pullRebaseFlow = appSettingsRepository.pullWithRebase
    val pushWithLeaseFlow = appSettingsRepository.pushWithLease
    val swapUncommittedChangesFlow = appSettingsRepository.swapUncommittedChangesFlow
    val cacheCredentialsInMemoryFlow = appSettingsRepository.cacheCredentialsInMemory
    val verifySslFlow = appSettingsRepository.verifySsl
    val terminalPathFlow = appSettingsRepository.terminalPath
    val avatarProviderFlow = appSettingsRepository.avatarProvider
    val dateFormatFlow = appSettingsRepository.dateTimeFormatFlow
    val proxyFlow = appSettingsRepository.proxyFlow

    fun setAppConfiguration(appConfiguration: AppConfiguration) = appScope.launch {
        appSettingsRepository.setConfiguration(appConfiguration)
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
}

@Stable
data class SettingsViewState(
    val scaleUi: Float,
    val theme: Theme,
    val linesHeightType: LinesHeightType,
    val defaultCloneDir: String,
    val fastForwardMerge: Boolean,
    val mergeAutoStash: Boolean,
    val pullWithRebase: Boolean,
    val pushWithLease: Boolean,
    val swapUncommittedChanges: Boolean,
    val cacheCredentialsInMemory: Boolean,
    val verifySsl: Boolean,
    val terminalPath: String?,
    val avatarProvider: AvatarProviderType,
    val dateFormat: String?,
    val proxy: ProxySettings,
)

// TODO Do we want something like this?
class GetConfigurationUseCase @Inject constructor(
    val appSettingsRepository: AppSettingsRepository,
) {
    inline fun <reified T : AppConfiguration> invoke(): Flow<T?> {
        return when (T::class) {
            AppConfiguration::ScaleUi::class -> appSettingsRepository.scaleUi
            AppConfiguration::LinesHeight::class -> appSettingsRepository.linesHeightType
            else -> throw IllegalStateException("Invalid settings class type")
        } as Flow<T?>
    }
}