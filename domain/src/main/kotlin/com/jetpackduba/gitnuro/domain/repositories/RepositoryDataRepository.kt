package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.Status
import kotlinx.coroutines.flow.Flow
import org.eclipse.jgit.lib.Ref

interface RepositoryDataRepository {
    val status: Flow<Status>
    val localBranches: Flow<List<Branch>>
    val currentBranch: Flow<Branch?>
    val tags: Flow<List<Ref>>
    val remotes: Flow<List<Remote>>
    val log: Flow<GraphCommitList>

    fun clearAll()
    fun updateStatus(status: Status)
    fun updateLocalBranches(branches: List<Branch>)
    fun updateCurrentBranch(branch: Branch?)
    fun updateTags(tags: List<Ref>)
    fun updateLog(graphCommitList: GraphCommitList)
    fun updateRemotes(remotes: List<Remote>)
}