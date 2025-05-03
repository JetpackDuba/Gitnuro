package com.jetpackduba.gitnuro.git.log


import com.jetpackduba.gitnuro.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.git.graph.GraphWalk
import com.jetpackduba.gitnuro.git.stash.GetStashListUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetLogUseCase @Inject constructor(
    private val getStashListUseCase: GetStashListUseCase,
) {
    suspend operator fun invoke(
        git: Git,
        currentBranch: Ref?,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int,
        cachedCommitList: GraphCommitList? = null,
    ) = withContext(Dispatchers.IO) {
        val repositoryState = git.repository.repositoryState
        val commitList = cachedCommitList ?: GraphCommitList()
        if (currentBranch != null || repositoryState.isRebasing) { // Current branch is null when there is no log (new repo) or rebasing
            val logList = git.log().setMaxCount(1).call().toList()

            if (cachedCommitList == null) {
                val walk = GraphWalk(git.repository)

                walk.use {
                    // Without this, during rebase conflicts the graph won't show the HEAD commits (new commits created
                    // by the rebase)
                    walk.markStart(walk.lookupCommit(logList.first()))

                    walk.markStartAllRefs(Constants.R_HEADS)
                    walk.markStartAllRefs(Constants.R_REMOTES)
                    walk.markStartAllRefs(Constants.R_TAGS)
                    walk.markStartStashes(getStashListUseCase(git))

                    if (hasUncommittedChanges) {
                        commitList.addUncommittedChangesGraphCommit(logList.first())
                    }

                    commitList.walker = walk
                }
            }

            commitList.fillTo(commitsLimit)

            ensureActive()
        }

        commitList.calcMaxLine()

        return@withContext commitList
    }
}