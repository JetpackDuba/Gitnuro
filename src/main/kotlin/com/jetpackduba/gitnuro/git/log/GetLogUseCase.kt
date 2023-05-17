package com.jetpackduba.gitnuro.git.log


import com.jetpackduba.gitnuro.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.git.graph.GraphWalk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetLogUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, currentBranch: Ref?, hasUncommitedChanges: Boolean, commitsLimit: Int) =
        withContext(Dispatchers.IO) {
            val commitList = GraphCommitList()
            val repositoryState = git.repository.repositoryState

            if (currentBranch != null || repositoryState.isRebasing) { // Current branch is null when there is no log (new repo) or rebasing
                val logList = git.log().setMaxCount(1).call().toList()

                //TODO: Perhaps cache GraphWalk when the commitsLimit is too big? This would ensure a fast traversal of the commits graph but would use more memory. Benchmark it and perhaps offer the option as a setting
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
}