package com.jetpackduba.gitnuro.data.git.log


import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.stash.GetStashListGitAction
import com.jetpackduba.gitnuro.data.mappers.GraphCommitMapper
import com.jetpackduba.gitnuro.domain.git.graph.*
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import kotlinx.coroutines.ensureActive
import org.eclipse.jgit.lib.Constants
import java.util.*
import javax.inject.Inject

val cachedGraphWalks = mutableMapOf<String, GraphWalk>()

class GetLogGitAction @Inject constructor(
    private val getStashListGitAction: GetStashListGitAction,
    private val graphCommitMapper: GraphCommitMapper,
    private val jgit: JGit,
) : IGetLogGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        currentBranch: Branch?,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int,
        currentData: GraphCommits?,
        isPaginated: Boolean,
    ) = jgit.provide(repositoryPath) { git ->
        val repositoryState = git.repository.repositoryState
        val cachedWalk = cachedGraphWalks[repositoryPath]
        if (currentBranch != null || repositoryState.isRebasing) { // Current branch is null when there is no log (new repo) or rebasing
            val logList = git.log().setMaxCount(1).call().toList()


            val walk = if (isPaginated && cachedWalk != null) {
                checkNotNull(cachedGraphWalks[repositoryPath])
            } else {
                if (!isPaginated) {
                    cachedWalk?.close()
                }

                GraphWalk(git.repository).apply {
                    val walk = this
                    // Without this, during rebase conflicts the graph won't show the HEAD commits (new commits created
                    // by the rebase)
                    walk.markStart(walk.lookupCommit(logList.first()))

                    walk.markStartAllRefs(Constants.R_HEADS)
                    walk.markStartAllRefs(Constants.R_REMOTES)
                    walk.markStartAllRefs(Constants.R_TAGS)
                    walk.markStartStashes(getStashListGitAction(git))
                }
            }

            val commitList = CommitsGraphGenerator(walk, currentData, graphCommitMapper)

            if (hasUncommittedChanges) {
                commitList.addUncommittedChangesGraphCommit(logList.first())
            }

            ensureActive()
            cachedGraphWalks[repositoryPath] = walk

            commitList.generateUpTo(commitsLimit)
        } else {
            GraphCommits(
                commits = LinkedHashMap(),
                maxLane = 0,
            )
        }

    }
}

