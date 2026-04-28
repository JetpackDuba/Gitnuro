package com.jetpackduba.gitnuro.data.git.log


import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.data.git.stash.GetStashListGitAction
import com.jetpackduba.gitnuro.data.mappers.GraphCommitMapper
import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.domain.git.graph.GraphWalk
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Constants
import javax.inject.Inject

class GetLogGitAction @Inject constructor(
    private val getStashListGitAction: GetStashListGitAction,
    private val graphCommitMapper: GraphCommitMapper,
) : IGetLogGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        currentBranch: Branch?,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int,
        cachedCommitList: GraphCommitList?,
    ) = withContext(Dispatchers.IO) {
        jgit(repositoryPath) {
            val repositoryState = repository.repositoryState
            val commitList = cachedCommitList ?: GraphCommitList()
            if (currentBranch != null || repositoryState.isRebasing) { // Current branch is null when there is no log (new repo) or rebasing
                val logList = log().setMaxCount(1).call().toList()

                if (cachedCommitList == null) {
                    val walk = GraphWalk(repository)

                    walk.use {
                        // Without this, during rebase conflicts the graph won't show the HEAD commits (new commits created
                        // by the rebase)
                        walk.markStart(walk.lookupCommit(logList.first()))

                        walk.markStartAllRefs(Constants.R_HEADS)
                        walk.markStartAllRefs(Constants.R_REMOTES)
                        walk.markStartAllRefs(Constants.R_TAGS)
                        walk.markStartStashes(getStashListGitAction(this))

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

            GraphCommits(
                commits = commitList.map { graphCommitMapper.toDomain(it) },
                maxLane = commitList.maxLane,
            )
        }
    }
}