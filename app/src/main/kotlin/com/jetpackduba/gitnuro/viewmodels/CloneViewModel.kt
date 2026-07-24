package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.interfaces.ICloneRepositoryGitAction
import com.jetpackduba.gitnuro.domain.models.AppConfig
import com.jetpackduba.gitnuro.domain.models.CloneState
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

class CloneViewModel @Inject constructor(
    private val cloneRepositoryGitAction: ICloneRepositoryGitAction,
    private val openFilePickerGitAction: OpenFilePickerGitAction,
    private val appSettings: AppSettingsService,
) : TabViewModel() {
    private val _repositoryUrl = MutableStateFlow(TextFieldValue(""))
    val repositoryUrl = _repositoryUrl.asStateFlow()

    // TODO Fix this
    private val _directoryPath = MutableStateFlow(TextFieldValue(/*appSettings.cloneDefaultDirectory*/))
    val directoryPath = _directoryPath.asStateFlow()

    private val _saveDirAsDefault = MutableStateFlow(false)
    val saveDirAsDefault = _saveDirAsDefault.asStateFlow()

    private val _folder = MutableStateFlow(TextFieldValue(""))
    val folder = _folder.asStateFlow()

    private val _cloneState = MutableStateFlow<CloneState>(CloneState.None)
    val cloneState = _cloneState.asStateFlow()

    val error: StateFlow<CloneUiError?>
        field = MutableStateFlow<CloneUiError?>(null)

    private var cloneJob: Job? = null

    fun clone(directoryPath: String, url: String, folder: String, cloneSubmodules: Boolean) {
        cloneJob = viewModelScope.launch {
            if (directoryPath.isBlank()) {
                error.value = CloneUiError.EmptyDirectory
                return@launch
            }

            if (url.isBlank()) {
                error.value = CloneUiError.EmptyUrl
                return@launch
            }

            if (folder.isBlank()) {
                error.value = CloneUiError.EmptyFolderName
                return@launch
            }

            val directory = File(directoryPath)

            if (!directory.exists()) {
                directory.mkdirs()
            }
            if (_saveDirAsDefault.value) {
                appSettings.setConfiguration(AppConfig.CloneDefaultDirectory(directoryPath))
            }

            val repoDir = File(directory, folder)
            if (!repoDir.exists()) {
                repoDir.mkdir()
            }

            cloneRepositoryGitAction(repoDir, url, cloneSubmodules)
                .flowOn(Dispatchers.IO)
                .collect { newCloneStatus ->
                    if (newCloneStatus is CloneState.Fail) {
                        error.value = CloneUiError.CloneError(newCloneStatus.reason)
                        _cloneState.value = CloneState.None
                    } else {
                        _cloneState.value = newCloneStatus
                    }
                }
        }
    }

    fun cancelClone() {
        viewModelScope.launch {
            _cloneState.value = CloneState.Cancelling
            cloneJob?.cancelAndJoin()
            _cloneState.value = CloneState.None
        }
    }

    fun resetStateIfError() {
        error.value = null
    }

    fun repoName(url: String): String {
        val urlSplit = url.split("/", "\\").toMutableList()

        // Removes the last element for URLs that end with "/" or "\" instead of the repo name like https://github.com/JetpackDuba/Gitnuro/
        if (urlSplit.isNotEmpty() && urlSplit.last().isBlank()) {
            urlSplit.removeLast()
        }

        // Take the last element of the path/URL to generate obtain the repo name
        return urlSplit.lastOrNull()?.removeSuffix(".git").orEmpty()
    }

    fun openDirectoryPicker(): String? {
        return openFilePickerGitAction(PickerType.DIRECTORIES, null)
    }

    fun onDirectoryPathChanged(directory: TextFieldValue) {
        _directoryPath.value = directory
    }

    fun onSaveAsDefaultChanged(value: Boolean) {
        _saveDirAsDefault.value = value
    }

    fun onRepositoryUrlChanged(repositoryUrl: TextFieldValue) {
        _repositoryUrl.value = repositoryUrl
    }

    fun onFolderNameChanged(folderName: TextFieldValue) {
        _folder.value = folderName
    }
}

sealed interface CloneUiError {
    data object EmptyDirectory : CloneUiError
    data object EmptyUrl : CloneUiError
    data object EmptyFolderName : CloneUiError
    data class CloneError(val error: AppError) : CloneUiError
}