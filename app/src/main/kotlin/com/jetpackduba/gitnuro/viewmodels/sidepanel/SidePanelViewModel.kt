package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndAdd
import com.jetpackduba.gitnuro.domain.extensions.toMutableSetAndRemove
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.usecases.*
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.ui.TabsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.submodule.SubmoduleStatus
import javax.inject.Inject

class SidePanelViewModel @Inject constructor(
    private val tabsManager: TabsManager,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val tabState: TabInstanceRepository,
    private val tabScope: TabCoroutineScope,
    private val fetchRemotesUseCase: FetchRemotesUseCase,
    private val setClipboardContentUseCase: SetClipboardContentUseCase,
    private val deleteRemoteInfoUseCase: DeleteRemoteInfoUseCase,
    private val checkoutCommitUseCase: CheckoutCommitUseCase,
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val rebaseBranchUseCase: RebaseBranchUseCase,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    private val deleteSubmoduleUseCase: DeleteSubmoduleUseCase,
    private val mergeBranchUseCase: MergeBranchUseCase,
    private val checkoutBranchUseCase: CheckoutBranchUseCase,
    private val updateSubmoduleUseCase: UpdateSubmoduleUseCase,
    private val syncSubmoduleUseCase: SyncSubmoduleUseCase,
    private val pushBranchUseCase: PushBranchUseCase,
    private val pullBranchUseCase: PullBranchUseCase,
    private val initializeSubmoduleUseCase: InitializeSubmoduleUseCase,
    private val deleteTagUseCase: DeleteTagUseCase,
    private val applyStashUseCase: ApplyStashUseCase,
    private val popStashUseCase: PopStashUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
) : TabViewModel() {

    val filter: StateFlow<String>
        field = MutableStateFlow("")

    val selectedItem: StateFlow<SelectedItem> = tabState.selectedItem

    val isExpandedBranches: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    val isExpandedRemotes: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(false)

    val isExpandedStashes: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    val isExpandedTags: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(false)

    val isExpandedSubmodules: StateFlow<Boolean>
        field = MutableStateFlow<Boolean>(true)

    val freeSearchFocusFlow: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>()

    private val branches = repositoryDataRepository.localBranches
    private val currentBranch = repositoryDataRepository.currentBranch

    val branchesState =
        combine(branches, currentBranch, isExpandedBranches, filter) { branches, currentBranch, isExpanded, filter ->
            BranchesState(
                branches = branches.filter { it.name.lowercaseContains(filter) },
                isExpanded = isExpanded,
                currentBranch = currentBranch
            )
        }.stateIn(BranchesState(emptyList(), isExpandedBranches.value, null))

    private val remotesContracted = MutableStateFlow<Set<Remote>>(emptySet())
    val remoteState: StateFlow<RemotesState> =
        combine(
            repositoryDataRepository.remotes,
            isExpandedRemotes,
            filter,
            currentBranch,
            remotesContracted,
        ) { remotes, isExpanded, filter, currentBranch, remotesContracted ->
            val remotesFiltered = remotes.map { remoteInfo ->
                val newRemoteInfo = remoteInfo.copy(
                    branchesList = remoteInfo.branchesList.filter { branch ->
                        branch.simpleName.lowercaseContains(filter)
                    }
                )

                RemoteView(newRemoteInfo, isExpanded = !remotesContracted.contains(newRemoteInfo.remote))
            }

            RemotesState(
                remotesFiltered,
                isExpanded,
                currentBranch
            )
        }.stateIn(RemotesState(emptyList(), isExpandedRemotes.value, null))

    val stashesState: StateFlow<StashesState> =
        combine(repositoryDataRepository.stashes, isExpandedStashes, filter) { stashes, isExpanded, filter ->
            StashesState(
                stashes = stashes.filter { it.message.lowercaseContains(filter) },
                isExpanded,
            )
        }.stateIn(StashesState(emptyList(), isExpandedStashes.value))

    val tagsState: StateFlow<TagsState> =
        combine(repositoryDataRepository.tags, isExpandedTags, filter) { tags, isExpanded, filter ->
            TagsState(
                tags.filter { tag -> tag.simpleName.lowercaseContains(filter) },
                isExpanded,
            )
        }.stateIn(TagsState(emptyList(), isExpandedTags.value))

    init {
        tabScope.launch {
            tabState.closeViewFlow.collectLatest {
                if (it == CloseableView.SIDE_PANE_SEARCH) {
                    newFilter("")
                    freeSearchFocusFlow.emit(Unit)
                }
            }
        }
    }

    fun newFilter(newValue: String) {
        filter.value = newValue
    }

    fun addSidePanelSearchToCloseables() = tabScope.launch {
        tabState.addCloseableView(CloseableView.SIDE_PANE_SEARCH)
    }

    fun removeSidePanelSearchFromCloseables() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.SIDE_PANE_SEARCH)
    }

    fun onExpandBranches() {
        isExpandedBranches.value = !isExpandedBranches.value
    }

    fun onExpandRemotes() {
        isExpandedRemotes.value = !isExpandedRemotes.value
    }

    fun onExpandSubmodules() {
        isExpandedSubmodules.value = !isExpandedSubmodules.value
    }

    fun onExpandTags() {
        isExpandedTags.value = !isExpandedTags.value
    }


    fun onRemoteClicked(remoteClicked: RemoteView) {
        remotesContracted.value = if (remotesContracted.value.contains(remoteClicked.remoteInfo.remote)) {
            remotesContracted.value.toMutableSetAndRemove(remoteClicked.remoteInfo.remote)
        } else {
            remotesContracted.value.toMutableSetAndAdd(remoteClicked.remoteInfo.remote)
        }
    }

    fun selectBranch(ref: Branch) {
        tabState.newSelectedRef(ref, ref.hash)
    }

    fun deleteRemote(remoteInfo: RemoteInfo) = deleteRemoteInfoUseCase(remoteInfo)

    fun onFetchRemoteBranches(remote: RemoteView) = fetchRemotesUseCase(remote.remoteInfo.remote)
    fun copyBranchNameToClipboard(branch: Branch) = viewModelScope.launch {
        setClipboardContentUseCase(branch.simpleName, MessageType.BranchCopied(branch.simpleName))
    }

    fun checkoutTagCommit(tag: Tag) = checkoutCommitUseCase(tag.hash)

    fun selectTag(tag: Tag) {
        // TODO Reimplement this
//        tabState.newSelectedRef(tag, tag.objectId)
    }

    private val _submodules = MutableStateFlow<List<Pair<String, SubmoduleStatus>>>(listOf())
    val submodules: StateFlow<SubmodulesState> =
        combine(_submodules, isExpandedSubmodules, filter) { submodules, isExpanded, filter ->
            SubmodulesState(
                submodules = submodules.filter { it.first.lowercaseContains(filter) },
                isExpanded = isExpanded
            )
        }.stateIn(SubmodulesState(emptyList(), isExpandedSubmodules.value))

    fun onOpenSubmoduleInTab(path: String) = viewModelScope.launch {
        val repositoryPath = repositoryDataRepository.repositoryPath

        if (repositoryPath != null) {
            // TODO Repository path may point to git dir and not workdir? If so, add use case
            tabsManager.addNewTabFromPath("$repositoryPath/$path", true)
        }
    }

    fun initializeSubmodule(path: String) = initializeSubmoduleUseCase(path)

    fun syncSubmodule(path: String) = syncSubmoduleUseCase(path)

    fun updateSubmodule(path: String) = updateSubmoduleUseCase(path)

    fun deleteSubmodule(path: String) = deleteSubmoduleUseCase(path)

    fun mergeBranch(branch: Branch) = mergeBranchUseCase(branch)

    fun deleteBranch(branch: Branch) = deleteBranchUseCase(branch)

    fun checkoutBranch(branch: Branch) = checkoutBranchUseCase(branch)

    fun rebaseBranch(branch: Branch) = rebaseBranchUseCase(branch)

    fun deleteRemoteBranch(branch: Branch) = deleteRemoteBranchUseCase(branch)

    fun checkoutRemoteBranch(remoteBranch: Branch) = checkoutBranchUseCase(remoteBranch)

    fun applyStash(stash: Commit) = applyStashUseCase(stash)
    fun popStash(stash: Commit) = popStashUseCase(stash)
    fun deleteStash(stash: Commit) = deleteStashUseCase(stash)

    fun pushToRemoteBranch(branch: Branch) = pushBranchUseCase(
        force = false,
        pushTags = false,
        targetRemoteBranch = branch
    )

    fun pullFromRemoteBranch(branch: Branch) = pullBranchUseCase(PullType.DEFAULT, branch)

    fun deleteTag(tag: Tag) = deleteTagUseCase(tag)
    fun selectStash(stash: Commit) {

    }
}

data class SubmodulesState(val submodules: List<Pair<String, SubmoduleStatus>>, val isExpanded: Boolean)

data class TagsState(val tags: List<Tag>, val isExpanded: Boolean)

data class StashesState(val stashes: List<Commit>, val isExpanded: Boolean)


data class BranchesState(
    val branches: List<Branch>,
    val isExpanded: Boolean,
    val currentBranch: Branch?,
)


data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)

data class RemotesState(val remotes: List<RemoteView>, val isExpanded: Boolean, val currentBranch: Branch?)