package git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

class LogManager {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loaded(listOf()))

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    suspend fun loadLog(git: Git) = withContext(Dispatchers.IO) {
        _logStatus.value = LogStatus.Loading

        val log: Iterable<RevCommit> = git.log().call()

        ensureActive()

        _logStatus.value = LogStatus.Loaded(log.toList())
    }
}

sealed class LogStatus {
    object Loading : LogStatus()
    data class Loaded(val commits: List<RevCommit>) : LogStatus()
}