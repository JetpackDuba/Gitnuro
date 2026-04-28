package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.interfaces.ICheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetTagsGitAction
import com.jetpackduba.gitnuro.domain.models.Tag
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.viewmodels.ISharedTagsViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedTagsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git

class TagsViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val checkoutCommitGitAction: ICheckoutCommitGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    tabScope: TabCoroutineScope,
    sharedTagsViewModel: SharedTagsViewModel,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(false), ISharedTagsViewModel by sharedTagsViewModel {
    private val tags = repositoryDataRepository.tags

    val tagsState: StateFlow<TagsState> = combine(tags, isExpanded, filter) { tags, isExpanded, filter ->
        TagsState(
            tags.filter { tag -> tag.simpleName.lowercaseContains(filter) },
            isExpanded,
        )
    }.stateIn(
        scope = tabScope,
        started = SharingStarted.Eagerly,
        initialValue = TagsState(emptyList(), isExpanded.value)
    )

    fun checkoutTagCommit(ref: Tag) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.InitSubmodule,
    ) { git ->
        checkoutCommitGitAction(git, ref.hash)

        positiveNotification("Commit checked out")
    }

    fun selectTag(tag: Tag) {
        // TODO Reimplement this
//        tabState.newSelectedRef(tag, tag.objectId)
    }
}

data class TagsState(val tags: List<Tag>, val isExpanded: Boolean)