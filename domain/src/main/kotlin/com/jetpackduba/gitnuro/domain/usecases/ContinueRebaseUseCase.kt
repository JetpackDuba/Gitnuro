package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IAbortRebaseGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IContinueRebaseGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class ContinueRebaseUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val continueRebaseGitAction: IContinueRebaseGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val getRepositoryStateGitAction: IGetRepositoryStateGitAction,
    private val getRebaseInteractiveStateGitAction: IGetRebaseInteractiveStateGitAction,
    private val doCommitUseCase: DoCommitUseCase,
) {
    operator fun invoke(message: String, isAmendRebaseInteractive: Boolean) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ContinueRebase,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            val repositoryState = getRepositoryStateGitAction(repositoryPath).bind()
            val rebaseInteractiveState = getRebaseInteractiveStateGitAction(repositoryPath).bind()

            if (
                repositoryState == RepositoryState.REBASING_INTERACTIVE &&
                rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits &&
                rebaseInteractiveState.isCurrentStepAmenable &&
                isAmendRebaseInteractive
            ) {
                val amendCommitId = rebaseInteractiveState.commitToAmendId

                if (!amendCommitId.isNullOrBlank()) {
                    doCommitUseCase(message, true, null/* TODO getIdentity(git)*/)
                }
            }

            continueRebaseGitAction(repositoryPath)
        }
    }
}