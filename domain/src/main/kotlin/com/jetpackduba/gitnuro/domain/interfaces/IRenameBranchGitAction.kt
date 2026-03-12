package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IRenameBranchGitAction {
    suspend operator fun invoke(git: Git, oldName: String, newName: String): Ref?
}