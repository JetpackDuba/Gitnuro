package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.rebase.AbortRebaseGitAction
import com.jetpackduba.gitnuro.domain.git.rebase.SkipRebaseGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class RebaseAbortUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val abortRebaseGitAction: AbortRebaseGitAction,
) {
    operator fun invoke() = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.ABORT_REBASE,
    ) { git ->
        abortRebaseGitAction(git)

        positiveNotification("Rebase aborted")
    }

}