package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetBranchesGitAction {
    // TODO after refactor remove this overload
    suspend operator fun invoke(git: Git): List<Ref>

    suspend operator fun invoke(repository: String): List<Ref>
}