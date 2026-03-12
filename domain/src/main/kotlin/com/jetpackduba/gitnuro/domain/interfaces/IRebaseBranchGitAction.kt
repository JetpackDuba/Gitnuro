package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

typealias IsMultiStep = Boolean

interface IRebaseBranchGitAction {
    suspend operator fun invoke(git: Git, ref: Ref): IsMultiStep
}