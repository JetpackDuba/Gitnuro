package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.usecases.ResetType
import org.eclipse.jgit.api.Git

interface IResetToCommitGitAction {
    suspend operator fun invoke(git: Git, commit: Commit, resetType: ResetType): Unit
}