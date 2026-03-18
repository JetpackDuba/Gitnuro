package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.mappers.JGitBranchMapper
import com.jetpackduba.gitnuro.domain.interfaces.IRenameBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class RenameBranchGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
) : IRenameBranchGitAction {
    override suspend operator fun invoke(git: Git, oldName: String, newName: String): Branch? = withContext(Dispatchers.IO) {
        val ref = git.branchRename()
            .setOldName(oldName)
            .setNewName(newName)
            .call()

        jGitBranchMapper.toDomain(ref)
    }
}