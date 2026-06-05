package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.domain.interfaces.IStageUntrackedFileGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStashChangesGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class StashChangesUseCase @Inject constructor(
    private val stageUntrackedFileGitAction: IStageUntrackedFileGitAction,
    private val stashChangesGitAction: IStashChangesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
) {
    operator fun invoke(message: String?) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Stash,
        onRefresh = {
            refreshStatusUseCase()
            refreshLogUseCase()
        }
    ) { repositoryPath ->
        stageUntrackedFileGitAction(repositoryPath).bind()

        stashChangesGitAction(repositoryPath, message?.nullIfEmpty)
    }
}