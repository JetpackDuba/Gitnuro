package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.ui.text.input.TextFieldValue
import com.jetpackduba.gitnuro.git.CloneState
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.remote_operations.CloneRepositoryUseCase
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
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
    private val tabState: TabState,
    private val cloneRepositoryUseCase: CloneRepositoryUseCase,
    private val openFilePickerUseCase: OpenFilePickerUseCase,
) {
    private val _repositoryUrl = MutableStateFlow(TextFieldValue(""))
    val repositoryUrl = _repositoryUrl.asStateFlow()

    private val _directoryPath = MutableStateFlow(TextFieldValue(""))
    val directoryPath = _directoryPath.asStateFlow()

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

            val repoDir = File(directory, folder)
            if (!repoDir.exists()) {
                repoDir.mkdir()
            }

            cloneRepositoryUseCase(repoDir, url, cloneSubmodules)
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
        return openFilePickerUseCase(PickerType.DIRECTORIES, null)
    }

    fun onDirectoryPathChanged(directory: TextFieldValue) {
        _directoryPath.value = directory
    }

    fun onRepositoryUrlChanged(repositoryUrl: TextFieldValue) {
        _repositoryUrl.value = repositoryUrl
    }

    fun onFolderNameChanged(folderName: TextFieldValue) {
        _folder.value = folderName
    }
}