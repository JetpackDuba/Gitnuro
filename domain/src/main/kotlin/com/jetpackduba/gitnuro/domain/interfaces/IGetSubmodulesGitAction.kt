package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatus

interface IGetSubmodulesGitAction {
    suspend operator fun invoke(git: Git): Map<String, SubmoduleStatus>
}