package app.viewmodels

import app.git.CloneStatus
import app.git.RemoteOperationsManager
import app.git.TabState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject

class CloneViewModel @Inject constructor(
    private val tabState: TabState,
    private val remoteOperationsManager: RemoteOperationsManager,
) {

    private val _cloneStatus = MutableStateFlow<CloneStatus>(CloneStatus.None)
    val cloneStatus: StateFlow<CloneStatus>
        get() = _cloneStatus

    var url: String = ""
    var directory: String = ""

    private var cloneJob: Job? = null

    fun clone(directoryPath: String, url: String) {
        cloneJob = tabState.safeProcessingWihoutGit {
            if (directoryPath.isBlank()) {
                _cloneStatus.value = CloneStatus.Fail("Check your URL and try again")
                return@safeProcessingWihoutGit
            }

            if (url.isBlank()) {
                _cloneStatus.value = CloneStatus.Fail("Check your URL and try again")
                return@safeProcessingWihoutGit
            }


            val urlSplit = url.split("/", "\\").toMutableList()

            // Removes the last element for URLs that end with "/" or "\" instead of the repo name like https://github.com/JetpackDuba/Gitnuro/
            if(urlSplit.isNotEmpty() && urlSplit.last().isBlank()) {
               urlSplit.removeLast()
            }

            // Take the last element of the path/URL to generate obtain the repo name
            val repoName = urlSplit.lastOrNull()?.replace(".git", "")

            if (repoName.isNullOrBlank()) {
                _cloneStatus.value = CloneStatus.Fail("Check your URL and try again")
                return@safeProcessingWihoutGit
            }

            val directory = File(directoryPath)

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val repoDir = File(directory, repoName)
            if (!repoDir.exists()) {
                repoDir.mkdir()
            }

            remoteOperationsManager.clone(repoDir, url)
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

    fun cancelClone() {
        cloneJob?.cancel()
        _cloneStatus.value = CloneStatus.Cancelling
    }
}