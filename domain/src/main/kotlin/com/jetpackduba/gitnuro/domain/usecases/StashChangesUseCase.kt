package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.domain.interfaces.IStageUntrackedFileGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IStashChangesGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.errorNotification
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class StashChangesUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val stageUntrackedFileGitAction: IStageUntrackedFileGitAction,
    private val stashChangesGitAction: IStashChangesGitAction,
) {
    operator fun invoke(message: String) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.Stash,
    ) { git ->
        stageUntrackedFileGitAction(git)

        if (stashChangesGitAction(git, message.nullIfEmpty)) {
            positiveNotification("Changes stashed")
        } else {
            errorNotification("There are no changes to stash")
        }
    }
}