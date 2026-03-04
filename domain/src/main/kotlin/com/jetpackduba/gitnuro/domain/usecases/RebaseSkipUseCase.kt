package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.rebase.SkipRebaseGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class RebaseSkipUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val skipRebaseGitAction: SkipRebaseGitAction,
) {
    operator fun invoke() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.SKIP_REBASE,
    ) { git ->
        skipRebaseGitAction(git)

        null
    }
}