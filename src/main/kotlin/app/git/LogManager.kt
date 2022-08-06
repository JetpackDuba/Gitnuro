package app.git

import app.git.graph.GraphCommitList
import app.git.graph.GraphWalk
import app.logging.printLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

private const val TAG = "LogManager"

class LogManager @Inject constructor() {
    suspend fun loadLog(git: Git, currentBranch: Ref?, hasUncommitedChanges: Boolean, commitsLimit: Int) =
        withContext(Dispatchers.IO) {
            val commitList = GraphCommitList()
            val repositoryState = git.repository.repositoryState

            printLog(TAG, "Repository state ${repositoryState.description}")

            if (currentBranch != null || repositoryState.isRebasing) { // Current branch is null when there is no log (new repo) or rebasing
                val logList = git.log().setMaxCount(1).call().toList()

                val walk = GraphWalk(git.repository)

                walk.use {
                    // Without this, during rebase conflicts the graph won't show the HEAD commits (new commits created
                    // by the rebase)
                    walk.markStart(walk.lookupCommit(logList.first()))

                    walk.markStartAllRefs(Constants.R_HEADS)
                    walk.markStartAllRefs(Constants.R_REMOTES)
                    walk.markStartAllRefs(Constants.R_TAGS)

                    if (hasUncommitedChanges)
                        commitList.addUncommitedChangesGraphCommit(logList.first())

                    commitList.source(walk)
                    commitList.fillTo(commitsLimit)
                }

                ensureActive()

            }

            commitList.calcMaxLine()

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

    suspend fun latestMessage(git: Git): String = withContext(Dispatchers.IO) {
        try {
            val log = git.log().setMaxCount(1).call()
            val latestCommitNode = log.firstOrNull()

            return@withContext if (latestCommitNode == null)
                ""
            else
                latestCommitNode.fullMessage

        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext ""
        }

    }

    suspend fun hasPreviousCommits(git: Git): Boolean = withContext(Dispatchers.IO) {
        try {
            val log = git.log().setMaxCount(1).call()
            val latestCommitNode = log.firstOrNull()

            return@withContext latestCommitNode != null

        } catch (ex: Exception) {
            ex.printStackTrace()
            return@withContext false
        }
    }
}

enum class ResetType {
    SOFT,
    MIXED,
    HARD,
}