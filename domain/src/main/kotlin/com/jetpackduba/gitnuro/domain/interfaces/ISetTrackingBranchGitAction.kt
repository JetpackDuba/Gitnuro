package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Branch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface ISetTrackingBranchGitAction {
    operator fun invoke(git: Git, branch: Branch, remoteName: String?, remoteBranch: Branch?)

    operator fun invoke(git: Git, refName: String, remoteName: String?, remoteBranchName: String?)
}