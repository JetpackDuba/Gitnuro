package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.Logging
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.git.signing.ClearRepositoryGitSigningOverridesUseCase
import com.jetpackduba.gitnuro.git.signing.DiscoverGitSigningKeysUseCase
import com.jetpackduba.gitnuro.git.signing.LoadGlobalGitSigningSettingsUseCase
import com.jetpackduba.gitnuro.git.signing.LoadRepositoryGitSigningSettingsUseCase
import com.jetpackduba.gitnuro.git.signing.SaveGlobalGitSigningSettingsUseCase
import com.jetpackduba.gitnuro.git.signing.SaveRepositoryGitSigningSettingsUseCase
import com.jetpackduba.gitnuro.git.signing.UnsetRepositoryGitSigningSettingUseCase
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.managers.Error
import com.jetpackduba.gitnuro.managers.newErrorNow
import com.jetpackduba.gitnuro.models.GitSigningKeyOption
import com.jetpackduba.gitnuro.models.GitSigningSettings
import com.jetpackduba.gitnuro.models.GitSigningSettingsField
import com.jetpackduba.gitnuro.models.GitSigningSettingsOverrides
import com.jetpackduba.gitnuro.models.GitSigningSettingsScope
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
import com.jetpackduba.gitnuro.system.PickerType
import com.jetpackduba.gitnuro.theme.LinesHeightType
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.ui.dialogs.settings.ProxyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.GpgConfig
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
    private val openFilePickerUseCase: OpenFilePickerUseCase,
    private val loadGlobalGitSigningSettingsUseCase: LoadGlobalGitSigningSettingsUseCase,
    private val loadRepositoryGitSigningSettingsUseCase: LoadRepositoryGitSigningSettingsUseCase,
    private val saveGlobalGitSigningSettingsUseCase: SaveGlobalGitSigningSettingsUseCase,
    private val saveRepositoryGitSigningSettingsUseCase: SaveRepositoryGitSigningSettingsUseCase,
    private val unsetRepositoryGitSigningSettingUseCase: UnsetRepositoryGitSigningSettingUseCase,
    private val clearRepositoryGitSigningOverridesUseCase: ClearRepositoryGitSigningOverridesUseCase,
    private val discoverGitSigningKeysUseCase: DiscoverGitSigningKeysUseCase,
    private val logging: Logging,
    @AppCoroutineScope private val appScope: CoroutineScope,
) {
    // Temporary values to detect changed variables
    var commitsLimit: Int = -1


    val themeState = appSettingsRepository.themeState
    val linesHeightTypeState = appSettingsRepository.linesHeightTypeState
    var defaultCloneDirFlow = appSettingsRepository.defaultCloneDirFlow
    val ffMergeFlow = appSettingsRepository.ffMergeFlow
    val mergeAutoStashFlow = appSettingsRepository.mergeAutoStashFlow
    val pullRebaseFlow = appSettingsRepository.pullRebaseFlow
    val pushWithLeaseFlow = appSettingsRepository.pushWithLeaseFlow
    val swapUncommittedChangesFlow = appSettingsRepository.swapUncommittedChangesFlow
    val cacheCredentialsInMemoryFlow = appSettingsRepository.cacheCredentialsInMemoryFlow
    val verifySslFlow = appSettingsRepository.verifySslFlow
    val terminalPathFlow = appSettingsRepository.terminalPathFlow
    val avatarProviderFlow = appSettingsRepository.avatarProviderTypeFlow
    val dateFormatFlow = appSettingsRepository.dateTimeFormatFlow
    val proxyFlow = appSettingsRepository.proxyFlow

    private val _gitSigningSettingsFlow = MutableStateFlow(GitSigningSettings())
    val gitSigningSettingsFlow = _gitSigningSettingsFlow.asStateFlow()

    private val _gitSigningOverridesFlow = MutableStateFlow(GitSigningSettingsOverrides())
    val gitSigningOverridesFlow = _gitSigningOverridesFlow.asStateFlow()

    private val _gitSigningScopeFlow = MutableStateFlow(GitSigningSettingsScope.GLOBAL)
    val gitSigningScopeFlow = _gitSigningScopeFlow.asStateFlow()

    private val _gitSigningRepositoryPathFlow = MutableStateFlow<String?>(null)
    val gitSigningRepositoryPathFlow = _gitSigningRepositoryPathFlow.asStateFlow()

    private val _gitSigningKeysFlow = MutableStateFlow<List<GitSigningKeyOption>>(emptyList())
    val gitSigningKeysFlow = _gitSigningKeysFlow.asStateFlow()

    private val _gitSigningKeysMessageFlow = MutableStateFlow("")
    val gitSigningKeysMessageFlow = _gitSigningKeysMessageFlow.asStateFlow()

    private val _isLoadingGitSigningKeysFlow = MutableStateFlow(false)
    val isLoadingGitSigningKeysFlow = _isLoadingGitSigningKeysFlow.asStateFlow()

    init {
        refreshGitSigningSettings()
    }

    var scaleUi: Float
        get() = appSettingsRepository.scaleUi
        set(value) {
            appSettingsRepository.scaleUi = value
        }

    var defaultCloneDir: String
        get() = appSettingsRepository.defaultCloneDir
        set(value) {
            appSettingsRepository.defaultCloneDir = value
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

    var mergeAutoStash: Boolean
        get() = appSettingsRepository.mergeAutoStash
        set(value) {
            appSettingsRepository.mergeAutoStash = value
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

    var avatarProvider
        get() = appSettingsRepository.avatarProviderType
        set(value) {
            appSettingsRepository.avatarProviderType = value
        }

    var dateFormat
        get() = appSettingsRepository.dateTimeFormat
        set(value) {
            appSettingsRepository.dateTimeFormat = value
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

    fun setGitSigningRepositoryPath(repositoryPath: String?) {
        val normalizedPath = repositoryPath?.ifBlank { null }
        val previousPath = _gitSigningRepositoryPathFlow.value

        if (previousPath == normalizedPath) {
            refreshGitSigningSettings()
            return
        }

        _gitSigningRepositoryPathFlow.value = normalizedPath

        if (normalizedPath == null && _gitSigningScopeFlow.value == GitSigningSettingsScope.REPOSITORY) {
            _gitSigningScopeFlow.value = GitSigningSettingsScope.GLOBAL
        }

        refreshGitSigningSettings()
    }

    fun setGitSigningScope(scope: GitSigningSettingsScope) {
        val normalizedScope = if (scope == GitSigningSettingsScope.REPOSITORY && _gitSigningRepositoryPathFlow.value == null) {
            GitSigningSettingsScope.GLOBAL
        } else {
            scope
        }

        if (_gitSigningScopeFlow.value == normalizedScope) {
            return
        }

        _gitSigningScopeFlow.value = normalizedScope
        refreshGitSigningSettings()
    }

    fun refreshGitSigningSettings() {
        appScope.launch {
            try {
                when (_gitSigningScopeFlow.value) {
                    GitSigningSettingsScope.GLOBAL -> {
                        _gitSigningSettingsFlow.value = loadGlobalGitSigningSettingsUseCase()
                        _gitSigningOverridesFlow.value = GitSigningSettingsOverrides()
                    }

                    GitSigningSettingsScope.REPOSITORY -> {
                        val repositoryPath = _gitSigningRepositoryPathFlow.value
                            ?: throw IllegalStateException("Repository path not available")
                        val repositorySettings = loadRepositoryGitSigningSettingsUseCase(repositoryPath)
                        _gitSigningSettingsFlow.value = repositorySettings.settings
                        _gitSigningOverridesFlow.value = repositorySettings.overrides
                    }
                }

                refreshGitSigningKeys(_gitSigningSettingsFlow.value)
            } catch (ex: Exception) {
                printError(TAG, "Unable to load Git signing settings", ex)
                _gitSigningKeysFlow.value = emptyList()
                _gitSigningKeysMessageFlow.value = when (_gitSigningScopeFlow.value) {
                    GitSigningSettingsScope.GLOBAL -> "Unable to load global Git signing settings."
                    GitSigningSettingsScope.REPOSITORY -> "Unable to load repository Git signing settings."
                }
                _isLoadingGitSigningKeysFlow.value = false
            }
        }
    }

    fun refreshGitSigningKeys() {
        appScope.launch {
            refreshGitSigningKeys(_gitSigningSettingsFlow.value)
        }
    }

    fun setGitSigningFormat(value: GpgConfig.GpgFormat) {
        val normalizedValue = if (value == GpgConfig.GpgFormat.SSH) {
            GpgConfig.GpgFormat.SSH
        } else {
            GpgConfig.GpgFormat.OPENPGP
        }

        markGitSigningFieldAsOverridden(GitSigningSettingsField.FORMAT)
        updateGitSigningSettings(
            _gitSigningSettingsFlow.value.copy(format = normalizedValue),
            refreshKeys = true,
        )
    }

    fun setGitSigningOpenPgpProgram(value: String) {
        markGitSigningFieldAsOverridden(GitSigningSettingsField.OPENPGP_PROGRAM)
        updateGitSigningSettings(
            _gitSigningSettingsFlow.value.copy(openPgpProgram = value),
        )
    }

    fun setGitSigningKey(value: String) {
        markGitSigningFieldAsOverridden(GitSigningSettingsField.SIGNING_KEY)
        updateGitSigningSettings(_gitSigningSettingsFlow.value.copy(signingKey = value))
    }

    fun setSignCommitsByDefault(value: Boolean) {
        markGitSigningFieldAsOverridden(GitSigningSettingsField.SIGN_COMMITS)
        updateGitSigningSettings(_gitSigningSettingsFlow.value.copy(signCommitsByDefault = value))
    }

    fun setSignTagsByDefault(value: Boolean) {
        markGitSigningFieldAsOverridden(GitSigningSettingsField.SIGN_TAGS)
        updateGitSigningSettings(_gitSigningSettingsFlow.value.copy(signTagsByDefault = value))
    }

    fun unsetRepositoryGitSigningSetting(field: GitSigningSettingsField) {
        val repositoryPath = _gitSigningRepositoryPathFlow.value ?: return
        if (_gitSigningScopeFlow.value != GitSigningSettingsScope.REPOSITORY) return

        appScope.launch {
            try {
                unsetRepositoryGitSigningSettingUseCase(repositoryPath, field)
                refreshGitSigningSettings()
            } catch (ex: Exception) {
                printError(TAG, "Unable to unset repository Git signing setting", ex)
            }
        }
    }

    fun clearRepositoryGitSigningOverrides() {
        val repositoryPath = _gitSigningRepositoryPathFlow.value ?: return
        if (_gitSigningScopeFlow.value != GitSigningSettingsScope.REPOSITORY) return

        appScope.launch {
            try {
                clearRepositoryGitSigningOverridesUseCase(repositoryPath)
                refreshGitSigningSettings()
            } catch (ex: Exception) {
                printError(TAG, "Unable to clear repository Git signing overrides", ex)
            }
        }
    }

    fun pickGitSigningOpenPgpProgram(): String? {
        val filePath = openFilePickerUseCase(PickerType.FILES, null)
        if (filePath != null) {
            setGitSigningOpenPgpProgram(filePath)
            refreshGitSigningKeys()
        }

        return filePath
    }

    fun pickGitSigningKeyFile(): String? {
        val filePath = openFilePickerUseCase(PickerType.FILES, null)
        if (filePath != null) {
            setGitSigningKey(filePath)
        }

        return filePath
    }

    private suspend fun refreshGitSigningKeys(settings: GitSigningSettings) {
        _isLoadingGitSigningKeysFlow.value = true

        try {
            val result = discoverGitSigningKeysUseCase(settings.format, settings.openPgpProgram)
            _gitSigningKeysFlow.value = result.options
            _gitSigningKeysMessageFlow.value = result.message
        } catch (ex: Exception) {
            printError(TAG, "Unable to discover signing keys", ex)
            _gitSigningKeysFlow.value = emptyList()
            _gitSigningKeysMessageFlow.value = "Unable to detect signing keys. You can still enter one manually."
        } finally {
            _isLoadingGitSigningKeysFlow.value = false
        }
    }

    private fun markGitSigningFieldAsOverridden(field: GitSigningSettingsField) {
        if (_gitSigningScopeFlow.value != GitSigningSettingsScope.REPOSITORY) {
            return
        }

        _gitSigningOverridesFlow.value = when (field) {
            GitSigningSettingsField.FORMAT -> _gitSigningOverridesFlow.value.copy(format = true)
            GitSigningSettingsField.OPENPGP_PROGRAM -> _gitSigningOverridesFlow.value.copy(openPgpProgram = true)
            GitSigningSettingsField.SIGNING_KEY -> _gitSigningOverridesFlow.value.copy(signingKey = true)
            GitSigningSettingsField.SIGN_COMMITS -> _gitSigningOverridesFlow.value.copy(signCommitsByDefault = true)
            GitSigningSettingsField.SIGN_TAGS -> _gitSigningOverridesFlow.value.copy(signTagsByDefault = true)
        }
    }

    private fun updateGitSigningSettings(
        settings: GitSigningSettings,
        refreshKeys: Boolean = false,
    ) {
        _gitSigningSettingsFlow.value = settings

        appScope.launch {
            try {
                when (_gitSigningScopeFlow.value) {
                    GitSigningSettingsScope.GLOBAL -> saveGlobalGitSigningSettingsUseCase(settings)
                    GitSigningSettingsScope.REPOSITORY -> {
                        val repositoryPath = _gitSigningRepositoryPathFlow.value
                            ?: throw IllegalStateException("Repository path not available")
                        saveRepositoryGitSigningSettingsUseCase(repositoryPath, settings)
                    }
                }

                if (refreshKeys) {
                    refreshGitSigningKeys(settings)
                }
            } catch (ex: Exception) {
                printError(TAG, "Unable to save Git signing settings", ex)
            }
        }
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