package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.usecases.ResetType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface IResetToCommitGitAction {
    suspend operator fun invoke(git: Git, revCommit: RevCommit, resetType: ResetType): Unit
}