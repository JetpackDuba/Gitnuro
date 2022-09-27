package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.extensions.isMerging
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.branches.GetCurrentBranchUseCase
import com.jetpackduba.gitnuro.git.repository.GetRepositoryStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import javax.inject.Inject

class GetDiffEntryForUncommitedDiffUseCase @Inject constructor(
    private val getRepositoryStateUseCase: GetRepositoryStateUseCase,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
) {
    suspend operator fun invoke(
        git: Git,
        diffEntryType: DiffEntryType.UncommitedDiff,
    ) = withContext(Dispatchers.IO) {
        val statusEntry = diffEntryType.statusEntry
        val cached = diffEntryType is DiffEntryType.StagedDiff
        val firstDiffEntry = git.diff()
            .setPathFilter(PathFilter.create(statusEntry.filePath))
            .setCached(cached).apply {
                val repositoryState = getRepositoryStateUseCase(git)
                if (
                    getCurrentBranchUseCase(git) == null &&
                    !repositoryState.isMerging &&
                    !repositoryState.isRebasing &&
                    cached
                ) {
                    setOldTree(EmptyTreeIterator()) // Required if the repository is empty
                }
            }
            .call()
            .firstOrNull()
            ?: throw MissingDiffEntryException("Diff entry not found")

        return@withContext firstDiffEntry
    }
}