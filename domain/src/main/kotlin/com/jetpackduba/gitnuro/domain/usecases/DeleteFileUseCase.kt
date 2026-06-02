package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteFileGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDiscardEntriesGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.TaskType
import java.io.File
import javax.inject.Inject

class DeleteFileUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val deleteFileGitAction: IDeleteFileGitAction,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
) {
    operator fun invoke(filePath: String) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DiscardFile,
            onRefresh = {
                refreshStatusUseCase()
                refreshLogUseCase()
            }
        ) { repositoryPath ->
            deleteFileGitAction(repositoryPath, filePath)
        }
    }
}
