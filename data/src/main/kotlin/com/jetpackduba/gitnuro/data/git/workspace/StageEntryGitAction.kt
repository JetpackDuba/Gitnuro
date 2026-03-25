package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IStageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import javax.inject.Inject

class StageEntryGitAction @Inject constructor() : IStageEntryGitAction {
    override suspend operator fun invoke(repositoryPath: String, statusEntry: StatusEntry): Either<Unit, AppError> {
        return jgit(repositoryPath) {
            add()
                .addFilepattern(statusEntry.filePath)
                .setUpdate(statusEntry.statusType == StatusType.REMOVED)
                .call()
        }
    }
}