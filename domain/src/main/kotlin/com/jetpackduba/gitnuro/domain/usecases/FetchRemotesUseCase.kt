package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IFetchAllRemotesGitAction
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class FetchRemotesUseCase @Inject constructor(
    private val fetchAllRemotesGitAction: IFetchAllRemotesGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshAllUseCase: RefreshAllUseCase,
) {
    operator fun invoke(specificRemote: Remote? = null) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.Fetch,
            onRefresh = {
                refreshAllUseCase()
            }
        ) { repositoryPath ->
            fetchAllRemotesGitAction(repositoryPath, specificRemote)
        }
    }
}