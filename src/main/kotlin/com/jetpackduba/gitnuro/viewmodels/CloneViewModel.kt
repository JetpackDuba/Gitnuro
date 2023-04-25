package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.CloneState
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.remote_operations.CloneRepositoryUseCase
import com.jetpackduba.gitnuro.system.OpenFilePickerUseCase
import com.jetpackduba.gitnuro.system.PickerType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class CloneViewModel @Inject constructor(
    private val tabState: TabState,
    private val cloneRepositoryUseCase: CloneRepositoryUseCase,
    private val openFilePickerUseCase: OpenFilePickerUseCase,
) {

    private val _cloneState = MutableStateFlow<CloneState>(CloneState.None)
    val cloneState: StateFlow<CloneState>
        get() = _cloneState

    var url: String = ""
    var directory: String = ""

    private var cloneJob: Job? = null

    fun clone(directoryPath: String, url: String, cloneSubmodules: Boolean) {
        cloneJob = tabState.safeProcessingWithoutGit {
            if (directoryPath.isBlank()) {
                _cloneState.value = CloneState.Fail("Invalid empty directory")
                return@safeProcessingWithoutGit
            }

            if (url.isBlank()) {
                _cloneState.value = CloneState.Fail("Invalid empty URL")
                return@safeProcessingWithoutGit
            }


            val urlSplit = url.split("/", "\\").toMutableList()

            // Removes the last element for URLs that end with "/" or "\" instead of the repo name like https://github.com/JetpackDuba/Gitnuro/
            if (urlSplit.isNotEmpty() && urlSplit.last().isBlank()) {
                urlSplit.removeLast()
            }

            // Take the last element of the path/URL to generate obtain the repo name
            var repoName = urlSplit.lastOrNull()

            if (repoName?.endsWith(".git") == true) {
                repoName = repoName.removeSuffix(".git")
            }

            if (repoName.isNullOrBlank()) {
                _cloneState.value = CloneState.Fail("Check your URL and try again")
                return@safeProcessingWithoutGit
            }

            val directory = File(directoryPath)

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val repoDir = File(directory, repoName)
            if (!repoDir.exists()) {
                repoDir.mkdir()
            }

            cloneRepositoryUseCase(repoDir, url, cloneSubmodules)
                .flowOn(Dispatchers.IO)
                .collect { newCloneStatus ->
                    _cloneState.value = newCloneStatus
                }
        }
    }

    fun reset() {
        _cloneState.value = CloneState.None
        url = ""
        directory = ""
    }

    fun cancelClone() = tabState.safeProcessingWithoutGit {
        _cloneState.value = CloneState.Cancelling
        cloneJob?.cancelAndJoin()
        _cloneState.value = CloneState.None
    }

    fun resetStateIfError() {
        _cloneState.value = CloneState.None
    }

    fun openDirectoryPicker(): String? {
        return openFilePickerUseCase(PickerType.DIRECTORIES, null)
    }
}