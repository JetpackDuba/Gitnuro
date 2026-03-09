package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.git.CloneState
import com.jetpackduba.gitnuro.domain.git.remote_operations.CloneRepositoryGitAction
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.system.OpenFilePickerGitAction
import com.jetpackduba.gitnuro.system.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class CloneViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val cloneRepositoryGitAction: CloneRepositoryGitAction,
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

    private val _error = MutableStateFlow("")
    val error = _error.asStateFlow()

    private var cloneJob: Job? = null

    fun clone(directoryPath: String, url: String, folder: String, cloneSubmodules: Boolean) {
        cloneJob = tabState.safeProcessingWithoutGit {
            if (directoryPath.isBlank()) {
                _error.value = "Invalid empty directory"
                return@safeProcessingWithoutGit
            }

            if (url.isBlank()) {
                _error.value = "Invalid empty URL"
                return@safeProcessingWithoutGit
            }

            if (folder.isBlank()) {
                _error.value = "Invalid empty folder name"
                return@safeProcessingWithoutGit
            }

            val directory = File(directoryPath)

            if (!directory.exists()) {
                directory.mkdirs()
            }
            if (_saveDirAsDefault.value) {
                // TODO Fix this
                // appSettings.defaultCloneDir = directoryPath
            }

            val repoDir = File(directory, folder)
            if (!repoDir.exists()) {
                repoDir.mkdir()
            }

            cloneRepositoryGitAction(repoDir, url, cloneSubmodules)
                .flowOn(Dispatchers.IO)
                .collect { newCloneStatus ->
                    if (newCloneStatus is CloneState.Fail) {
                        _error.value = newCloneStatus.reason
                        _cloneState.value = CloneState.None
                    } else {
                        _cloneState.value = newCloneStatus
                    }
                }
        }
    }

    fun cancelClone() = tabState.safeProcessingWithoutGit {
        _cloneState.value = CloneState.Cancelling
        cloneJob?.cancelAndJoin()
        _cloneState.value = CloneState.None
    }

    fun resetStateIfError() {
        _error.value = ""
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