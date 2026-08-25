package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.PersistedCommitMessage

interface IGetPersistedCommitMessagesGitAction {
    suspend operator fun invoke(repositoryPath: String): Either<PersistedCommitMessage, AppError>
}