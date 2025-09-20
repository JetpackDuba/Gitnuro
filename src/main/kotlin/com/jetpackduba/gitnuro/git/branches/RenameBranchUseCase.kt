package com.jetpackduba.gitnuro.git.branches

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RenameBranchUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, oldName: String, newName: String): Ref? = withContext(Dispatchers.IO) {
        git.branchRename()
            .setOldName(oldName)
            .setNewName(newName)
            .call()
    }
}