package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.nullIf
import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.git.workspace.StageAllGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.domain.git.workspace.UnstageAllGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class StatusUnstageAllUseCase @Inject constructor(
    private val unstageAllGitAction: UnstageAllGitAction,
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