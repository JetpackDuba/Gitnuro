package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.GraphCommits

interface IGetLogGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        currentBranch: Branch?,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int,
        currentData: GraphCommits? = null,
        isPaginated: Boolean,
    ): Either<GraphCommits, AppError>
}