package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.CloneStatus
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.remote_operations.CloneRepositoryUseCase
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
) {

    private val _cloneStatus = MutableStateFlow<CloneStatus>(CloneStatus.None)
    val cloneStatus: StateFlow<CloneStatus>
        get() = _cloneStatus

    var url: String = ""
    var directory: String = ""

    private var cloneJob: Job? = null

    fun clone(directoryPath: String, url: String, cloneSubmodules: Boolean) {
        cloneJob = tabState.safeProcessingWithoutGit {
            if (directoryPath.isBlank()) {
                _cloneStatus.value = CloneStatus.Fail("Invalid empty directory")
                return@safeProcessingWithoutGit
            }

            if (url.isBlank()) {
                _cloneStatus.value = CloneStatus.Fail("Invalid empty URL")
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
                _cloneStatus.value = CloneStatus.Fail("Check your URL and try again")
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
                    _cloneStatus.value = newCloneStatus
                }
        }
    }

    fun reset() {
        _cloneStatus.value = CloneStatus.None
        url = ""
        directory = ""
    }

    fun cancelClone() = tabState.safeProcessingWithoutGit {
        _cloneStatus.value = CloneStatus.Cancelling
        cloneJob?.cancelAndJoin()
        _cloneStatus.value = CloneStatus.None
    }

    fun resetStateIfError() {
        _cloneStatus.value = CloneStatus.None
    }
}