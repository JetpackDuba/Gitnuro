package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.domain.interfaces.IRenameBranchGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RenameBranchGitAction @Inject constructor() : IRenameBranchGitAction {
    override suspend operator fun invoke(git: Git, oldName: String, newName: String): Ref? = withContext(Dispatchers.IO) {
        git.branchRename()
            .setOldName(oldName)
            .setNewName(newName)
            .call()
    }
}