package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.ui.UiDataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

data class SubmodulesState(val isLoading: Boolean, val submodules: List<Pair<String, Submodule>>, val isExpanded: Boolean)

data class TagsState(val tags: List<Tag>, val isExpanded: Boolean)

data class StashesState(val stashes: List<Commit>, val isExpanded: Boolean)


data class BranchesState(
    val isLoading: Boolean,
    val branches: List<Branch>,
    val isExpanded: Boolean,
    val currentBranch: Branch?,
)

fun combineBranchesState(
    branches: Flow<UiDataState<List<Branch>>>,
    currentBranch: Flow<UiDataState<Branch?>>,
    isExpandedBranches: MutableStateFlow<Boolean>,
    filter: MutableStateFlow<String>
): Flow<BranchesState> {
    return combine(branches, currentBranch, isExpandedBranches, filter) { branches, currentBranch, isExpanded, filter ->
        BranchesState(
            isLoading = branches.isLoading || currentBranch.isLoading,
            branches = branches.data
                .orEmpty()
                .filter { it.name.lowercaseContains(filter) }
                .sortedWith { branch, branch1 ->
                    if (branch == currentBranch) return@sortedWith -1
                    if (branch1 == currentBranch) return@sortedWith 1
                    else branch.name.compareTo(branch1.name, ignoreCase = true)
                },
            isExpanded = isExpanded,
            currentBranch = currentBranch.data
        )
    }
}


data class RemoteView(val remoteInfo: RemoteInfo, val isExpanded: Boolean)

data class RemotesState(
    val remotes: List<RemoteView> = emptyList(),
    val isExpanded: Boolean = false,
    val currentBranch: Branch? = null
)

fun combineRemotesState(
    remotes: StateFlow<UiDataState<List<RemoteInfo>>>,
    isExpandedRemotes: MutableStateFlow<Boolean>,
    filter: MutableStateFlow<String>,
    currentBranch: Flow<UiDataState<Branch?>>,
    remotesContracted: MutableStateFlow<Set<Remote>>,
): Flow<RemotesState> {
    return combine(
        remotes,
        isExpandedRemotes,
        filter,
        currentBranch,
        remotesContracted,
    ) { remotes, isExpanded, filter, currentBranch, remotesContracted ->
        val remotesFiltered = remotes.data.orEmpty().map { remoteInfo ->
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
            currentBranch.data,
        )
    }
}