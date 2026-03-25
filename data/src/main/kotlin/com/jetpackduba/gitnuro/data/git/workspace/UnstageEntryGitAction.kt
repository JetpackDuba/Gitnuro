package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import javax.inject.Inject

class UnstageEntryGitAction @Inject constructor() : IUnstageEntryGitAction {
    override suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry): Either<Unit, AppError> {
        return jgit(repositoryPath) {
            reset()
                .addPath(statusEntry.filePath)
                .call()

            Unit
        }
    }
}