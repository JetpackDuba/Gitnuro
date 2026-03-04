package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.extensions.nullIf
import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.git.workspace.StageAllGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.StageEntryGitAction
import com.jetpackduba.gitnuro.domain.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.domain.git.workspace.UnstageEntryGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class StatusUnstageUseCase @Inject constructor(
    private val unstageEntryGitAction: UnstageEntryGitAction,
    private val tabState: TabInstanceRepository,
) {
    operator fun invoke(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageEntryGitAction(git, statusEntry)
    }
}