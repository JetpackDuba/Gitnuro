package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.branches.CheckoutRefUseCase
import com.jetpackduba.gitnuro.git.tags.DeleteTagUseCase
import com.jetpackduba.gitnuro.git.tags.GetTagsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class TagsViewModel @Inject constructor(
    private val tabState: TabState,
    private val getTagsUseCase: GetTagsUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val checkoutRefUseCase: CheckoutRefUseCase,
) : ExpandableViewModel() {
    private val _tags = MutableStateFlow<List<Ref>>(listOf())
    val tags: StateFlow<List<Ref>>
        get() = _tags

    private suspend fun loadTags(git: Git) = withContext(Dispatchers.IO) {
        val tagsList = getTagsUseCase(git)

        _tags.value = tagsList
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        checkoutRefUseCase(git, ref)
    }

    fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        deleteTagUseCase(git, tag)
    }

    fun selectTag(tag: Ref) {
        tabState.newSelectedRef(tag.objectId)
    }

    suspend fun refresh(git: Git) {
        loadTags(git)
    }
}