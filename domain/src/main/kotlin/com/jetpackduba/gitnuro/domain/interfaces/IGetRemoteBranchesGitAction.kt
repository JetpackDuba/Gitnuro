package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetRemoteBranchesGitAction {
    suspend operator fun invoke(git: Git): List<Ref>
}