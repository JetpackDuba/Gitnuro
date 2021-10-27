package app.git

import app.git.graph.GraphCommitList
import app.git.graph.GraphWalk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject


class LogManager @Inject constructor(
    private val statusManager: StatusManager,
) {
    private val _logStatus = MutableStateFlow<LogStatus>(LogStatus.Loading)

    val logStatus: StateFlow<LogStatus>
        get() = _logStatus

    suspend fun loadLog(git: Git) = withContext(Dispatchers.IO) {
        _logStatus.value = LogStatus.Loading

        val logList = git.log().setMaxCount(2).call().toList()

        val commitList = GraphCommitList()
        val walk = GraphWalk(git.repository)

        walk.use {
            walk.markStartAllRefs(Constants.R_HEADS)
            walk.markStartAllRefs(Constants.R_REMOTES)
            walk.markStartAllRefs(Constants.R_TAGS)

            if (statusManager.checkHasUncommitedChanges(git))
                commitList.addUncommitedChangesGraphCommit(logList.first())

            commitList.source(walk)
            commitList.fillTo(1000) // TODO: Limited commits to show to 1000, add a setting to let the user adjust this
        }

        ensureActive()

        val loadedStatus = LogStatus.Loaded(commitList)

        _logStatus.value = loadedStatus
    }

    suspend fun checkoutCommit(git: Git, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setName(revCommit.name)
            .call()
    }

    suspend fun createBranchOnCommit(git: Git, branch: String, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setCreateBranch(true)
            .setName(branch)
            .setStartPoint(revCommit)
            .call()
    }

    suspend fun createTagOnCommit(git: Git, tag: String, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .tag()
            .setAnnotated(true)
            .setName(tag)
            .setObjectId(revCommit)
            .call()
    }
}


sealed class LogStatus {
    object Loading : LogStatus()
    class Loaded(val plotCommitList: GraphCommitList) : LogStatus()
}