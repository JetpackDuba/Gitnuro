package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface ICheckHasPreviousCommitsGitAction {
    suspend operator fun invoke(git: Git): Boolean
}