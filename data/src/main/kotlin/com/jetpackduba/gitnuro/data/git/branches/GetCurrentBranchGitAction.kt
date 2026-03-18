package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.mappers.JGitBranchMapper
import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

/**
 * Returns the current branch in [Ref]. If the repository is new, the current branch will be null.
 */
class GetCurrentBranchGitAction @Inject constructor(
    private val getBranchesGitAction: GetBranchesGitAction,
    private val branchMapper: JGitBranchMapper,
) : IGetCurrentBranchGitAction {
    override suspend operator fun invoke(git: Git): Branch? {
        return invoke(git.repository.directory.absolutePath)
    }
    override suspend operator fun invoke(path: String): Branch? {
        return jgit(path) {
            val branchList = getBranchesGitAction(this)
            val branchName =
                repository
                .fullBranch

            var branchFound = branchList.firstOrNull {
                it.name == branchName
            }

            if (branchFound == null) {
                branchFound = branchList.firstOrNull {
                    it.hash == branchName // Try to get the HEAD
                }
            }

            return@jgit branchFound
        }
    }
}