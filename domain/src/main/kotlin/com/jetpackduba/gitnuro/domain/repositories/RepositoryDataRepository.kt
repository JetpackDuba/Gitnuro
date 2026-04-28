package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.lib.Ref

interface RepositoryDataRepository {
    val status: Flow<Status>
    val localBranches: Flow<List<Branch>>
    val currentBranch: Flow<Branch?>
    val tags: Flow<List<Tag>>
    val remotes: Flow<List<RemoteInfo>>
    val log: Flow<GraphCommits>
    val stashes: Flow<List<Commit>>
    val repositoryState: StateFlow<RepositorySelectionState>
    val repositoryPath: String?
    val diffSelected: StateFlow<DiffSelected?>

    fun setRepositoryState(state: RepositorySelectionState)
    fun clearAll()
    fun updateStatus(status: Status)
    fun updateLocalBranches(branches: List<Branch>)
    fun updateCurrentBranch(branch: Branch?)
    fun updateTags(tags: List<Tag>)
    fun updateLog(graphCommits: GraphCommits)
    fun updateRemotes(remotes: List<RemoteInfo>)
    fun updateDiffSelected(diffSelected: DiffSelected?)
    fun updateStashes(stashes: List<Commit>)
}