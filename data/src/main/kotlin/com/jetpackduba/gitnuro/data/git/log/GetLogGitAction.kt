package com.jetpackduba.gitnuro.data.git.log


import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.stash.GetStashListGitAction
import com.jetpackduba.gitnuro.data.mappers.GraphCommitMapper
import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.domain.git.graph.GraphWalk
import com.jetpackduba.gitnuro.domain.interfaces.IGetLogGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommit
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import kotlinx.coroutines.ensureActive
import org.eclipse.jgit.lib.Constants
import javax.inject.Inject

private val graphsCache = mutableMapOf<String, Pair<GraphWalk, GraphCommitList>>()

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
        cachedCommitList: GraphCommitList?,
    ) = jgit.provide(repositoryPath) { git ->

        val pair = graphsCache[repositoryPath]

        if (pair != null) {
            val (walkCached, commitListCached) = pair
            //if (walkCached.markedRoots == walk.markedRoots) {
                val commits = LinkedHashMap<String, GraphCommit>(commitListCached.size)

                for (entry in commitListCached) {
                    commits[entry.name] = graphCommitMapper.toDomain(entry)
                }

                return@provide GraphCommits(
                    commits = commits,
                    maxLane = commitListCached.maxLane,
                )
            //}
        }


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
                    walk.markStartStashes(getStashListGitAction(git))

                    //if (hasUncommittedChanges) {
                    commitList.addUncommittedChangesGraphCommit(logList.first())
                    //}

                    commitList.walker = walk
                }

                graphsCache[repositoryPath] = walk to commitList
            }

            commitList.fillTo(commitsLimit)

            ensureActive()
        }

        commitList.calcMaxLine()

        val commits = LinkedHashMap<String, GraphCommit>(commitList.size)

        for (entry in commitList) {
            commits[entry.name] = graphCommitMapper.toDomain(entry)
        }

        GraphCommits(
            commits = commits,
            maxLane = commitList.maxLane,
        )
    }
}