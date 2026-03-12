package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IResetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class RepositoryResetStateUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val resetRepositoryStateGitAction: IResetRepositoryStateGitAction,
) {
    operator fun invoke() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RESET_REPO_STATE,
    ) { git ->
        resetRepositoryStateGitAction(git)

        positiveNotification("Repository state has been reset")
    }
}