package com.jetpackduba.gitnuro.domain.git.branches

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

/**
 * Returns the current branch in [Ref]. If the repository is new, the current branch will be null.
 */
class GetCurrentBranchGitAction @Inject constructor(
    private val getBranchesGitAction: GetBranchesGitAction,
) {
    suspend operator fun invoke(git: Git): Ref? {
        val branchList = getBranchesGitAction(git)
        val branchName = git
            .repository
            .fullBranch

        var branchFound = branchList.firstOrNull {
            it.name == branchName
        }

        if (branchFound == null) {
            branchFound = branchList.firstOrNull {
                it.objectId.name == branchName // Try to get the HEAD
            }
        }

        return branchFound
    }
}