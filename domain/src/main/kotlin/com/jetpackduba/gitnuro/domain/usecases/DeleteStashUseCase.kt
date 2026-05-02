package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class DeleteStashUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val deleteStashGitAction: IDeleteStashGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val refreshStashListUseCase: RefreshStashListUseCase,
) {
    operator fun invoke(stash: Commit) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Stash,
        refreshEvenIfFailed = true,
        onRefresh = {
            refreshLogUseCase()
            refreshStashListUseCase()
        }
    ) { repositoryPath ->
        deleteStashGitAction(repositoryPath, stash)
    }
}