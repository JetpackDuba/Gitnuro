package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.submodule.SubmoduleStatus
import javax.inject.Inject

class InMemoryRepositoryDataRepository @Inject constructor() : RepositoryDataRepository {
    override val status: Flow<Status>
        field = MutableStateFlow<Status>(Status(emptyList(), emptyList(), emptyList()))

    override val localBranches: Flow<List<Branch>>
        field = MutableStateFlow<List<Branch>>(emptyList())

    override val currentBranch: Flow<Branch?>
        field = MutableStateFlow<Branch?>(null)

    override val tags: Flow<List<Tag>>
        field = MutableStateFlow(emptyList())

    override val remotes: Flow<List<RemoteInfo>>
        field = MutableStateFlow(emptyList())

    override val log: StateFlow<GraphCommits>
        field = MutableStateFlow(GraphCommits(LinkedHashMap(), 0))

    override val stashes: Flow<List<Commit>>
        field = MutableStateFlow(emptyList())

    override val submodules: Flow<Map<String, SubmoduleStatus>>
        field = MutableStateFlow(emptyMap())

    override val repositorySelectionState: StateFlow<RepositorySelectionState>
        field = MutableStateFlow<RepositorySelectionState>(RepositorySelectionState.Unknown)

    override val repositoryState: StateFlow<RepositoryState>
        field = MutableStateFlow<RepositoryState>(RepositoryState.SAFE)

    override val rebaseInteractiveState: StateFlow<RebaseInteractiveState>
        field = MutableStateFlow<RebaseInteractiveState>(RebaseInteractiveState.None)

    override val author: Flow<AuthorInfo>
        field = MutableStateFlow(AuthorInfo(emptyIdentity(), emptyIdentity()))

    override var maxCommitsToLoadLimit: Int = 0

    override val repositoryPath: String?
        get() {
            return when (val state = repositorySelectionState.value) {
                is RepositorySelectionState.Open -> state.path
                else -> null
            }
        }

    override fun setRepositoryState(state: RepositorySelectionState) {
        repositorySelectionState.value = state
    }

    override fun clearAll() {
        this.status.value = Status(emptyList(), emptyList(), emptyList())
        this.localBranches.value = emptyList()
        this.tags.value = emptyList()
        this.remotes.value = emptyList()
        this.log.value = GraphCommits(LinkedHashMap(), 0)
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

    override fun updateTags(tags: List<Tag>) {
        this.tags.value = tags
    }

    override fun updateLog(graphCommits: GraphCommits) {
        this.log.value = graphCommits
    }

    override fun updateRemotes(remotes: List<RemoteInfo>) {
        this.remotes.value = remotes
    }

    override fun updateStashes(stashes: List<Commit>) {
        this.stashes.value = stashes
    }

    override fun updateSubmodules(value: Map<String, SubmoduleStatus>) {
        this.submodules.value = value
    }

    override fun updateAuthor(value: AuthorInfo) {
        this.author.value = value
    }
}
