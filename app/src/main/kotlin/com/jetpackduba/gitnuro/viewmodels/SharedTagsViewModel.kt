package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.data.git.log.CheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteTagGitAction
import com.jetpackduba.gitnuro.domain.models.Tag
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.Job
import javax.inject.Inject

interface ISharedTagsViewModel {
    fun deleteTag(tag: Tag): Job
    fun checkoutTag(tag: Tag): Job
}

class SharedTagsViewModel @Inject constructor(
    private val deleteTagGitAction: IDeleteTagGitAction,
    private val checkoutCommitGitAction: CheckoutCommitGitAction,
    private val tabState: TabInstanceRepository,
) : ISharedTagsViewModel {
    override fun deleteTag(tag: Tag) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Tag delete",
        subtitle = "Deleting tag ${tag.simpleName}",
        taskType = TaskType.DeleteTag,
    ) { git ->
        deleteTagGitAction(git, tag)

        positiveNotification("Tag \"${tag.simpleName}\" deleted")
    }
    override fun checkoutTag(tag: Tag) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        title = "Tag checkout",
        subtitle = "Checking out commit of tag ${tag.simpleName}",
        taskType = TaskType.DeleteTag,
    ) { git ->
        checkoutCommitGitAction(git, tag.hash)

        positiveNotification("Tag \"${tag.simpleName    }\" deleted")
    }
}
