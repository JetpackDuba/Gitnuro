package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface ISetTrackingBranchGitAction {
    operator fun invoke(git: Git, ref: Ref, remoteName: String?, remoteBranch: Ref?)

    operator fun invoke(git: Git, refName: String, remoteName: String?, remoteBranchName: String?)
}