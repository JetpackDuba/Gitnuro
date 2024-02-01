package com.jetpackduba.gitnuro.git.log


import com.jetpackduba.gitnuro.git.graph.GenerateLogWalkUseCase
import com.jetpackduba.gitnuro.git.graph.GraphCommitList2
import com.jetpackduba.gitnuro.git.stash.GetStashListUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import javax.inject.Inject

class GetLogUseCase @Inject constructor(
    private val getStashListUseCase: GetStashListUseCase,
    private val generateLogWalkUseCase: GenerateLogWalkUseCase,
) {
    suspend operator fun invoke(git: Git, hasUncommittedChanges: Boolean, commitsLimit: Int?) =
        withContext(Dispatchers.IO) {
            val logList = git.log().setMaxCount(1).call().toList()
            val firstCommit = logList.firstOrNull()
            val allRefs =
                git.repository.refDatabase.refs.filterNot { it.name.startsWith(Constants.R_STASH) } // remove stash as it only returns the latest, we get all afterward
            val stashes = getStashListUseCase(git)

            return@withContext if (firstCommit == null) {
                GraphCommitList2(emptyList(), 0)
            } else {
                generateLogWalkUseCase.invoke(
                    git,
                    firstCommit,
                    allRefs,
                    stashes,
                    hasUncommittedChanges,
                    commitsLimit
                )
            }
        }
}