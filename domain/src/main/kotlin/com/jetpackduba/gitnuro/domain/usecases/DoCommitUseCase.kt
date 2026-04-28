package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.SignOffConstants
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IDoCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.ILoadSignOffConfigGitAction
import com.jetpackduba.gitnuro.domain.models.Identity
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DoCommitUseCase @Inject constructor(
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val doCommitGitAction: IDoCommitGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val loadSignOffConfigGitAction: ILoadSignOffConfigGitAction,
    private val getAuthorUseCase: GetAuthorUseCase,
) {
    operator fun invoke(
        message: String,
        amend: Boolean,
        author: Identity?,
    ) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DoCommit,
            onRefresh = {
                refreshStatusUseCase()
                refreshLogUseCase()
            }
        ) { repositoryPath ->
            val signOffConfig = loadSignOffConfigGitAction(repositoryPath).bind()

            val finalMessage = if (signOffConfig.isEnabled) {
                val authorToSign = author ?: getAuthorUseCase().bind().toIdentity()

                val signature = signOffConfig.format
                    .replace(SignOffConstants.DEFAULT_SIGN_OFF_FORMAT_USER, authorToSign.name)
                    .replace(SignOffConstants.DEFAULT_SIGN_OFF_FORMAT_EMAIL, authorToSign.email)

                "$message\n\n$signature"
            } else
                message


            doCommitGitAction(repositoryPath, finalMessage, amend, author)
        }
    }
}
