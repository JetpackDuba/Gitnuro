package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.DiffSelected
import com.jetpackduba.gitnuro.domain.models.GraphCommits
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import com.jetpackduba.gitnuro.domain.models.RepositorySelectionState
import com.jetpackduba.gitnuro.domain.models.Status
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class InMemoryRepositoryDataRepository @Inject constructor() : RepositoryDataRepository {
    override val status: Flow<Status>
        field = MutableStateFlow<Status>(Status(emptyList(), emptyList()))

    override val localBranches: Flow<List<Branch>>
        field = MutableStateFlow<List<Branch>>(emptyList())

    override val currentBranch: Flow<Branch?>
        field = MutableStateFlow<Branch?>(null)

    override val tags: Flow<List<Ref>>
        field = MutableStateFlow(emptyList())

    override val remotes: Flow<List<RemoteInfo>>
        field = MutableStateFlow(emptyList())

    override val log: Flow<GraphCommits>
        field = MutableStateFlow(GraphCommits(emptyList(), 0))

    override val repositoryState: StateFlow<RepositorySelectionState>
        field = MutableStateFlow<RepositorySelectionState>(RepositorySelectionState.Unknown)

    override val repositoryPath: String?
        get() {
            return when (val state = repositoryState.value) {
                is RepositorySelectionState.Open -> state.path
                else -> null
            }
        }

    override val diffSelected: StateFlow<DiffSelected?>
        field = MutableStateFlow<DiffSelected?>(null)

    override fun setRepositoryState(state: RepositorySelectionState) {
        repositoryState.value = state
    }

    override fun clearAll() {
        this.status.value = Status(emptyList(), emptyList())
        this.localBranches.value = emptyList()
        this.tags.value = emptyList()
        this.remotes.value = emptyList()
        this.log.value = GraphCommits(emptyList(), 0)
    }

    override fun updateStatus(status: Status) {
        this.status.value = status
    }

    override fun updateLocalBranches(branches: List<Branch>) {
        this.localBranches.value = branches
    }

    override fun updateCurrentBranch(branch: Branch?) {
        this.currentBranch.value = branch
    }

    override fun updateTags(tags: List<Ref>) {
        this.tags.value = tags
    }

    override fun updateLog(graphCommits: GraphCommits) {
        this.log.value = graphCommits
    }

    override fun updateRemotes(remotes: List<RemoteInfo>) {
        this.remotes.value = remotes
    }

    override fun updateDiffSelected(diffSelected: DiffSelected?) {
        this.diffSelected.value = diffSelected
    }
}
