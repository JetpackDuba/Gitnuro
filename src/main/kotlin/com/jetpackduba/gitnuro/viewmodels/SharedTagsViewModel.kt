package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.CheckoutRefUseCase
import com.jetpackduba.gitnuro.models.positiveNotification
import kotlinx.coroutines.Job
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

interface ISharedTagsViewModel {
    fun deleteTag(tag: Ref): Job
}

class SharedTagsViewModel @Inject constructor(
    private val deleteTagUseCase: CheckoutRefUseCase,
    private val tabState: TabState,
) : ISharedTagsViewModel {
    override fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Tag delete",
        subtitle = "Deleting tag ${tag.simpleName}",
        taskType = TaskType.DELETE_TAG,
    ) { git ->
        deleteTagUseCase(git, tag)

        positiveNotification("Tag \"${tag.simpleName}\" deleted")
    }
}
