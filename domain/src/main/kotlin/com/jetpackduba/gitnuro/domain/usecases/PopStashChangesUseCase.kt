package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.GenericError
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.raiseError
import com.jetpackduba.gitnuro.domain.interfaces.IGetStashListGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPopStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class PopStashUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val popStashGitAction: IPopStashGitAction,
    private val getStashListGitAction: IGetStashListGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
) {
    operator fun invoke(commit: Commit?) = useCaseExecutor.executeLaunch(
        taskType = TaskType.Stash,
        refreshEvenIfFailed = true,
        onRefresh = {
            refreshStatusUseCase()
            refreshLogUseCase()
        }
    ) { repositoryPath ->
        val stashCommit = commit ?: getStashListGitAction(repositoryPath).bind().firstOrNull()

        if (stashCommit == null) {
            raiseError(GenericError("No stashes found")) // TODO Refactor this to a proper type
        }

        popStashGitAction(repositoryPath, stashCommit)
    }
}