package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.ICreateTagGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import javax.inject.Inject

class CreateTagUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val createTagGitAction: ICreateTagGitAction,
) {
    operator fun invoke(tag: String, revCommit: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "New tag",
        subtitle = "Creating new tag \"$tag\" on commit ${revCommit.shortHash}",
        taskType = TaskType.CreateTag,
    ) { git ->
        createTagGitAction(git, tag, revCommit)

        positiveNotification("Tag created")
    }
}