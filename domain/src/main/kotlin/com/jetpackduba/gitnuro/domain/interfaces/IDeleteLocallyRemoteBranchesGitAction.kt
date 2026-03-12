package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IDeleteLocallyRemoteBranchesGitAction {
    suspend operator fun invoke(git: Git, branches: List<String>): List<String>
}