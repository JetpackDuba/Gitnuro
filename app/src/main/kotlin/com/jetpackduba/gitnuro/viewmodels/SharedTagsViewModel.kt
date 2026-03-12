package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteTagGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.Job
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

interface ISharedTagsViewModel {
    fun deleteTag(tag: Ref): Job
}

class SharedTagsViewModel @Inject constructor(
    private val deleteTagGitAction: IDeleteTagGitAction,
    private val tabState: TabInstanceRepository,
) : ISharedTagsViewModel {
    override fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Tag delete",
        subtitle = "Deleting tag ${tag.simpleName}",
        taskType = TaskType.DELETE_TAG,
    ) { git ->
        deleteTagGitAction(git, tag)

        positiveNotification("Tag \"${tag.simpleName}\" deleted")
    }
}
