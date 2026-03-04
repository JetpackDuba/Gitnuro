package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.Status
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class InMemoryRepositoryDataRepository @Inject constructor() : RepositoryDataRepository {
    override val status: Flow<Status>
        field = MutableStateFlow<Status>(Status(emptyList(), emptyList()))

    override val localBranches: Flow<List<Ref>>
        field = MutableStateFlow<List<Ref>>(emptyList())

    override val tags: Flow<List<Ref>>
        field = MutableStateFlow(emptyList())

    override val remotes: Flow<List<Remote>>
        field = MutableStateFlow(emptyList())

    override val log: Flow<GraphCommitList>
        field = MutableStateFlow(GraphCommitList())

    override fun clearAll() {
        this.status.value = Status(emptyList(), emptyList())
        this.localBranches.value = emptyList()
        this.tags.value = emptyList()
        this.remotes.value = emptyList()
        this.log.value = GraphCommitList()
    }

    override fun updateStatus(status: Status) {
        this.status.value = status
    }

    override fun updateLocalBranches(branches: List<Ref>) {
        this.localBranches.value = branches
    }

    override fun updateTags(tags: List<Ref>) {
        this.tags.value = tags
    }

    override fun updateLog(graphCommitList: GraphCommitList) {
        this.log.value = graphCommitList
    }
}