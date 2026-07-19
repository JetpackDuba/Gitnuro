package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RepositoryDataRepository {
    val status: Flow<Status>
    val localBranches: Flow<List<Branch>>
    val currentBranch: Flow<Branch?>
    val tags: Flow<List<Tag>>
    val remotes: Flow<List<RemoteInfo>>
    val log: StateFlow<GraphCommits>
    val stashes: Flow<List<Commit>>
    val repositorySelectionState: StateFlow<RepositorySelectionState>
    val repositoryState: StateFlow<RepositoryState>
    val rebaseInteractiveState: StateFlow<List<RebaseLine>>
    val repositoryPath: String?
    val submodules: Flow<Map<String, Submodule>>
    val author: Flow<AuthorInfo>
    var maxCommitsToLoadLimit: Int

    fun setRepositorySelectionState(state: RepositorySelectionState)
    fun clearAll()
    fun updateStatus(status: Status)
    fun updateLocalBranches(branches: List<Branch>)
    fun updateCurrentBranch(branch: Branch?)
    fun updateTags(tags: List<Tag>)
    fun updateLog(graphCommits: GraphCommits)
    fun updateRemotes(remotes: List<RemoteInfo>)
    fun updateStashes(stashes: List<Commit>)
    fun updateSubmodules(value: Map<String, Submodule>)
    fun updateAuthor(value: AuthorInfo)
    fun updateRepositoryState(value: RepositoryState)
    fun updateRebaseInteractiveState(value: List<RebaseLine>)
}