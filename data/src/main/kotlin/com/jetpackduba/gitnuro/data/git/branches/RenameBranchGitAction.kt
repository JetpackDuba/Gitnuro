package com.jetpackduba.gitnuro.data.git.branches

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitBranchMapper
import com.jetpackduba.gitnuro.domain.interfaces.IRenameBranchGitAction
import javax.inject.Inject

class RenameBranchGitAction @Inject constructor(
    private val jGitBranchMapper: JGitBranchMapper,
    private val jgit: JGit,
) : IRenameBranchGitAction {
    override suspend operator fun invoke(repositoryPath: String, oldName: String, newName: String) =
        jgit.provide(repositoryPath) { git ->
            val ref = git.branchRename()
                .setOldName(oldName)
                .setNewName(newName)
                .call()

            checkNotNull(jGitBranchMapper.toDomain(ref)) { "Failed to map $ref to domain branch" }
        }
}