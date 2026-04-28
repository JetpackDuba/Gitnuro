package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IUnstageByDirectoryGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class UnstageByDirectoryUseCase @Inject constructor(
    private val unstageByDirectoryGitAction: IUnstageByDirectoryGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshStatusUseCase: RefreshStatusUseCase,
) {
    operator fun invoke(dir: String) = useCaseExecutor.executeLaunch(
        taskType = TaskType.StageDir,
        onRefresh = {
            refreshStatusUseCase()
        }
    ) { repositoryPath ->
        unstageByDirectoryGitAction(repositoryPath, dir)
    }
}