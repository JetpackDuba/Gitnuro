package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IDeleteBranchGitAction {
    suspend operator fun invoke(git: Git, branch: Ref): List<String>
}