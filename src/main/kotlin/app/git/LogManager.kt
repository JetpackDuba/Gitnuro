package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revplot.PlotCommit
import org.eclipse.jgit.revplot.PlotCommitList
import org.eclipse.jgit.revplot.PlotLane
import org.eclipse.jgit.revplot.PlotWalk
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject

class LogManager @Inject constructor() {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    suspend fun loadLog(git: Git) = withContext(Dispatchers.IO) {
        _logStatus.value = LogStatus.Loading

        val commitList = PlotCommitList<PlotLane>()
        val walk = PlotWalk(git.repository)

        walk.markStart(walk.parseCommit(git.repository.resolve("HEAD")));
        commitList.source(walk)

        commitList.fillTo(Int.MAX_VALUE)

        ensureActive()

        _logStatus.value = LogStatus.Loaded(commitList)
    }
}

sealed class LogStatus {
    object Loading : LogStatus()
    data class Loaded(val plotCommitList: PlotCommitList<PlotLane>) : LogStatus()
}