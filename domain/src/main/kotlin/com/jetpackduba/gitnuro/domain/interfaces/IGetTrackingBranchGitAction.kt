package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.TrackingBranch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetTrackingBranchGitAction {
    operator fun invoke(git: Git, ref: Ref): TrackingBranch?

    operator fun invoke(git: Git, refName: String): TrackingBranch?
}