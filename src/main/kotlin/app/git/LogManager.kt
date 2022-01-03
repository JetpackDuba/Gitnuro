package app.git

import app.extensions.isBranch
import app.extensions.isMerging
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
) {
    suspend fun loadLog(git: Git, currentBranch: Ref?) = withContext(Dispatchers.IO) {
        val commitList = GraphCommitList()
        val repositoryState = git.repository.repositoryState

        if(currentBranch != null || repositoryState.isRebasing) { // Current branch is null when there is no log (new repo) or rebasing
            val logList = git.log().setMaxCount(2).call().toList()

            val walk = GraphWalk(git.repository)

            walk.use {
                walk.markStartAllRefs(Constants.R_HEADS)
                walk.markStartAllRefs(Constants.R_REMOTES)
                walk.markStartAllRefs(Constants.R_TAGS)

                if (statusManager.hasUncommitedChanges(git))
                    commitList.addUncommitedChangesGraphCommit(logList.first())

                commitList.source(walk)
                commitList.fillTo(1000) // TODO: Limited commits to show to 1000, add a setting to let the user adjust this
            }

            ensureActive()

        }

        return@withContext commitList
    }

    suspend fun checkoutCommit(git: Git, revCommit: RevCommit) = withContext(Dispatchers.IO) {
        git
            .checkout()
            .setName(revCommit.name)
            .call()
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