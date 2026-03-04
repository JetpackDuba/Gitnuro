package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.nullIf
import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.git.repository.ResetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.StageAllGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.UnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class RepositoryResetStateUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val resetRepositoryStateGitAction: ResetRepositoryStateGitAction,
) {
    operator fun invoke() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RESET_REPO_STATE,
    ) { git ->
        resetRepositoryStateGitAction(git)

        positiveNotification("Repository state has been reset")
    }
}