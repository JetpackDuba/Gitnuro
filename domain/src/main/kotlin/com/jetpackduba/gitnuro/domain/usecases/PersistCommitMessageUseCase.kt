package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IPersistCommitMessageGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class PersistCommitMessageUseCase @Inject constructor(
    private val persistCommitMessageGitAction: IPersistCommitMessageGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    operator fun invoke(message: String?) {
        val messageToPersist = message?.ifBlank { null }
        useCaseExecutor.executeLaunch(
            taskType = TaskType.PersistCommitMessage,
            dataToRefresh = emptyArray(),
        ) { repositoryPath ->
            persistCommitMessageGitAction(repositoryPath, messageToPersist)
        }
    }
}
