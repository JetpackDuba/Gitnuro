package com.jetpackduba.gitnuro.data.git.diff

import com.jetpackduba.gitnuro.domain.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.data.git.branches.GetCurrentBranchGitAction
import com.jetpackduba.gitnuro.data.git.repository.GetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.FileVersion
import com.jetpackduba.gitnuro.domain.interfaces.IGetDiffEntryFromStatusEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import javax.inject.Inject

class GetDiffEntryFromStatusEntryGitAction @Inject constructor(
    private val getRepositoryStateGitAction: GetRepositoryStateGitAction,
    private val getCurrentBranchGitAction: GetCurrentBranchGitAction,
) : IGetDiffEntryFromStatusEntryGitAction {
    override suspend operator fun invoke(
        git: Git,
        isCached: Boolean,
        statusEntry: StatusEntry,
        fileVersion: FileVersion,
    ) = withContext(Dispatchers.IO) {
        val pathFilter = when(fileVersion) {
            FileVersion.OLD -> statusEntry.oldPath
            FileVersion.NEW -> statusEntry.newPath
        }

        val firstDiffEntry = git.diff()
            .setPathFilter(PathFilter.create(pathFilter))
            .setCached(isCached).apply {
                val repositoryState = getRepositoryStateGitAction(git.repository.directory.absolutePath).okOrNull()!!
                if (
                    getCurrentBranchGitAction(git).okOrNull() == null &&
                    !repositoryState.isMerging &&
                    !repositoryState.isRebasing &&
                    isCached
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