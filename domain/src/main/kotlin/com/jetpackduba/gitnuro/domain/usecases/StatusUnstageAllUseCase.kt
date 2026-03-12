package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IUnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class StatusUnstageAllUseCase @Inject constructor(
    private val unstageAllGitAction: IUnstageAllGitAction,
    private val tabState: TabInstanceRepository,
) {
    operator fun invoke(entries: List<StatusEntry>?) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        taskType = TaskType.UNSTAGE_ALL_FILES,
    ) { git ->
        unstageAllGitAction(git, entries)

        null
    }
}