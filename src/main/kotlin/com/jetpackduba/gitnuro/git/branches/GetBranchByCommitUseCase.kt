package com.jetpackduba.gitnuro.git.branches

import com.jetpackduba.gitnuro.extensions.isBranch
import com.jetpackduba.gitnuro.extensions.isHead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetBranchByCommitUseCase @Inject constructor() {

    suspend operator fun invoke(git: Git, commit: RevCommit): Ref = withContext(Dispatchers.IO) {
        git
            .branchList()
            .setContains(commit.id.name)
            .setListMode(ListBranchCommand.ListMode.ALL)
            .call()
            .first { it.isBranch }
    }

}