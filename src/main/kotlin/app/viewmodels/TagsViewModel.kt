package app.viewmodels

import app.git.BranchesManager
import app.git.RefreshType
import app.git.TabState
import app.git.TagsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class TagsViewModel @Inject constructor(
    private val tabState: TabState,
    private val branchesManager: BranchesManager,
    private val tagsManager: TagsManager,
) {
    private val _tags = MutableStateFlow<List<Ref>>(listOf())
    val tags: StateFlow<List<Ref>>
        get() = _tags

    private suspend fun loadTags(git: Git) = withContext(Dispatchers.IO) {
        val tagsList = tagsManager.getTags(git)

        _tags.value = tagsList
    }

    fun checkoutRef(ref: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        branchesManager.checkoutRef(git, ref)
    }

    fun deleteTag(tag: Ref) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
    ) { git ->
        tagsManager.deleteTag(git, tag)
    }

    suspend fun refresh(git: Git) {
        loadTags(git)
    }
}