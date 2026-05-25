package com.jetpackduba.gitnuro.viewmodels.sidepanel

import com.jetpackduba.gitnuro.domain.extensions.lowercaseContains
import com.jetpackduba.gitnuro.domain.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import org.eclipse.jgit.submodule.SubmoduleStatus

data class SubmodulesState(val submodules: List<Pair<String, SubmoduleStatus>>, val isExpanded: Boolean)

data class TagsState(val tags: List<Tag>, val isExpanded: Boolean)

data class StashesState(val stashes: List<Commit>, val isExpanded: Boolean)


data class BranchesState(
    val branches: List<Branch>,
    val isExpanded: Boolean,
    val currentBranch: Branch?,
)

fun combineBranchesState(
    branches: Flow<List<Branch>>,
    currentBranch: Flow<Branch?>,
    isExpandedBranches: MutableStateFlow<Boolean>,
    filter: MutableStateFlow<String>
): Flow<BranchesState> {
    return combine(branches, currentBranch, isExpandedBranches, filter) { branches, currentBranch, isExpanded, filter ->
        BranchesState(
            branches = branches.filter { it.name.lowercaseContains(filter) },
            isExpanded = isExpanded,
            currentBranch = currentBranch
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
    remotes: Flow<List<RemoteInfo>>,
    isExpandedRemotes: MutableStateFlow<Boolean>,
    filter: MutableStateFlow<String>,
    currentBranch: Flow<Branch?>,
    remotesContracted: MutableStateFlow<Set<Remote>>,
): Flow<RemotesState> {
    return combine(
        remotes,
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
    }
}