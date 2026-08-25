package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.interfaces.IGetPersistedCommitMessagesGitAction
import com.jetpackduba.gitnuro.domain.models.PersistedCommitMessage
import javax.inject.Inject

class GetPersistedCommitMessagesGitAction @Inject constructor(
    private val jgit: JGit,
) : IGetPersistedCommitMessagesGitAction {
    override suspend fun invoke(repositoryPath: String): Either<PersistedCommitMessage, AppError> {
        return jgit.provide(repositoryPath) { git ->
            val commitMessage = git.repository.readCommitEditMsg()
            val mergeMessage = git.repository.readMergeCommitMsg()
            val squashMessage = git.repository.readSquashCommitMsg()

            PersistedCommitMessage(commitMessage, mergeMessage, squashMessage)
        }
    }
}