package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IContinueRebaseGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.Identity
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.models.RepositoryState
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class ContinueRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val continueRebaseGitAction: IContinueRebaseGitAction,
    private val doCommitUseCase: DoCommitUseCase,
) {
    operator fun invoke(
        message: String,
        isAmendRebaseInteractive: Boolean,
        repositoryState: RepositoryState,
        rebaseInteractiveState: RebaseInteractiveState,
        onIdentityRequest: suspend () -> Identity?,
    ) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ContinueRebase,
            dataToRefresh = arrayOf(DataToRefresh.ALL),
        ) { repositoryPath ->
            if (
                repositoryState == RepositoryState.REBASING_INTERACTIVE &&
                rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits &&
                rebaseInteractiveState.isCurrentStepAmenable &&
                isAmendRebaseInteractive
            ) {
                val amendCommitId = rebaseInteractiveState.commitToAmendId

                if (!amendCommitId.isNullOrBlank()) {
                    doCommitUseCase(message, true, onIdentityRequest())
                }
            }

            continueRebaseGitAction(repositoryPath)
        }
    }
}