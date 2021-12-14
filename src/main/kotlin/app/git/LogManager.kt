package app.git

import app.extensions.isBranch
import app.extensions.simpleName
import app.git.graph.GraphCommitList
import app.git.graph.GraphWalk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CreateBranchCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject


class LogManager @Inject constructor(
    private val statusManager: StatusManager,
    private val branchesManager: BranchesManager,
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

        val loadedStatus = LogStatus.Loaded(commitList, branchesManager.currentBranchRef(git))

        _logStatus.value = loadedStatus
    }

    suspend fun checkoutCommit(git: Git, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setName(revCommit.name)
            .call()
    }

    suspend fun checkoutRef(git: Git, ref: Ref) = withContext(Dispatchers.IO) {
        git.checkout().apply {
            setName(ref.name)
            if (ref.isBranch && ref.name.startsWith("refs/remotes/")) {
                setCreateBranch(true)
                setName(ref.simpleName)
                setStartPoint(ref.objectId.name)
                setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
            }
            call()
        }
    }

    suspend fun revertCommit(git: Git, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .revert()
            .include(revCommit)
            .call()
    }

    suspend fun resetToCommit(git: Git, revCommit: RevCommit, resetType: ResetType) = withContext(Dispatchers.IO) {
        val reset = when (resetType) {
            ResetType.SOFT -> ResetCommand.ResetType.SOFT
            ResetType.MIXED -> ResetCommand.ResetType.MIXED
            ResetType.HARD -> ResetCommand.ResetType.HARD
        }
        git
            .reset()
            .setMode(reset)
            .setRef(revCommit.name)
            .call()
    }
}

// TODO Move this to
enum class ResetType {
    SOFT,
    MIXED,
    HARD,
}

sealed class LogStatus {
    object Loading : LogStatus()
    class Loaded(val plotCommitList: GraphCommitList, val currentBranch: Ref?) : LogStatus()
}