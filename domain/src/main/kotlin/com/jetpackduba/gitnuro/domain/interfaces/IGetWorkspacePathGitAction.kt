package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git

interface IGetWorkspacePathGitAction {
    operator fun invoke(git: Git): String
}