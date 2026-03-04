package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.git.workspace.StageAllGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class StatusStageAllUseCase @Inject constructor(
    private val stageAllGitAction: StageAllGitAction,
    private val tabState: TabInstanceRepository,
) {
    operator fun invoke(entries: List<StatusEntry>?) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        taskType = TaskType.STAGE_ALL_FILES,
    ) { git ->
        stageAllGitAction(git, entries)

        null
    }
}