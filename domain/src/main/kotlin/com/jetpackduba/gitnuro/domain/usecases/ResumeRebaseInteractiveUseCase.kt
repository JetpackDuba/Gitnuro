package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IResumeRebaseInteractiveGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import org.eclipse.jgit.api.RebaseCommand
import javax.inject.Inject

class ResumeRebaseInteractiveUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val resumeRebaseInteractiveGitAction: IResumeRebaseInteractiveGitAction,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(interactiveHandler: RebaseCommand.InteractiveHandler) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.RebaseInteractive,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            resumeRebaseInteractiveGitAction(repositoryPath, interactiveHandler)
        }
    }
}