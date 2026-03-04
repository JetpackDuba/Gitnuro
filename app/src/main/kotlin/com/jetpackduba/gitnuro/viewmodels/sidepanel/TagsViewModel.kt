package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.extensions.simpleName
import com.jetpackduba.gitnuro.domain.git.log.CheckoutCommitGitAction
import com.jetpackduba.gitnuro.domain.git.tags.GetTagsGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.viewmodels.ISharedTagsViewModel
import com.jetpackduba.gitnuro.viewmodels.SharedTagsViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

class TagsViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val getTagsGitAction: GetTagsGitAction,
    private val checkoutCommitGitAction: CheckoutCommitGitAction,
    tabScope: CoroutineScope,
    sharedTagsViewModel: SharedTagsViewModel,
    @Assisted
    private val filter: StateFlow<String>,
) : SidePanelChildViewModel(false), ISharedTagsViewModel by sharedTagsViewModel {
    private val tags = MutableStateFlow<List<Ref>>(listOf())

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

    init {
        tabScope.launch {
            tabState.refreshFlowFiltered(RefreshType.ALL_DATA, RefreshType.STASHES)
            {
                refresh(tabState.git)
            }
        }
    }

    private suspend fun loadTags(git: Git) = withContext(Dispatchers.IO) {
        val tagsList = getTagsGitAction(git)

        tags.value = tagsList
    }

    fun checkoutTagCommit(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.INIT_SUBMODULE,
    ) { git ->
        checkoutCommitGitAction(git, ref.objectId.name)

        positiveNotification("Commit checked out")
    }

    fun selectTag(tag: Ref) {
        tabState.newSelectedRef(tag, tag.objectId)
    }

    suspend fun refresh(git: Git) {
        loadTags(git)
    }
}

data class TagsState(val tags: List<Ref>, val isExpanded: Boolean)