package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetLogGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        currentBranch: Branch?,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int,
        cachedCommitList: GraphCommitList? = null,
    ): Either<GraphCommits, AppError>
}