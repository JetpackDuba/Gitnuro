package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.TrackingBranch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetTrackingBranchGitAction {
    suspend operator fun invoke(repositoryPath: String, branch: Branch): Either<TrackingBranch?, GitError>
    suspend operator fun invoke(repositoryPath: String, refName: String): Either<TrackingBranch?, GitError>
}