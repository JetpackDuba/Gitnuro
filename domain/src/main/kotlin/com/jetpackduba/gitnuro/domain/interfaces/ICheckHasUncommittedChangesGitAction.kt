package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface ICheckHasUncommittedChangesGitAction {
    suspend operator fun invoke(git: Git): Boolean
}