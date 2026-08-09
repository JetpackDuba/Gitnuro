package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.TAG
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.onErr
import com.jetpackduba.gitnuro.domain.interfaces.IPersistCommitMessageGitAction
import javax.inject.Inject

class PersistCommitMessageUseCase @Inject constructor(
    private val persistCommitMessageGitAction: IPersistCommitMessageGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(message: String?) {
        val messageToPersist = message?.ifBlank { null }
        useCaseExecutor.execute { repositoryPath ->
            val result = persistCommitMessageGitAction(repositoryPath, messageToPersist)
                .onErr {
                    printError(TAG, "Failed to persist commit message: $it")
                }

            result
        }
    }
}
