package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface RepositoryDataRepository {
    val status: Flow<DataState<Status>>
    val localBranches: Flow<DataState<List<Branch>>>
    val currentBranch: Flow<DataState<Branch?>>
    val tags: Flow<DataState<List<Tag>>>
    val remotes: Flow<DataState<List<RemoteInfo>>>
    val log: StateFlow<DataState<GraphCommits>>
    val stashes: Flow<DataState<List<Commit>>>
    val repositorySelectionState: StateFlow<RepositorySelectionState>
    val repositoryState: StateFlow<DataState<RepositoryState>>
    val rebaseInteractiveState: StateFlow<DataState<List<RebaseLine>>>
    val repositoryPath: String?
    val submodules: Flow<DataState<Map<String, Submodule>>>
    val author: Flow<DataState<AuthorInfo>>
    var maxCommitsToLoadLimit: Int

    fun setRepositorySelectionState(state: RepositorySelectionState)
    fun clearAll()
    suspend fun updateStatus(block: suspend () -> Either<Status, AppError>)
    suspend fun updateLocalBranches(block: suspend () -> Either<List<Branch>, AppError>)
    suspend fun updateCurrentBranch(block: suspend () -> Either<Branch?, AppError>)
    suspend fun updateTags(block: suspend () -> Either<List<Tag>, AppError>)
    suspend fun updateLog(block: suspend () -> Either<GraphCommits, AppError>)
    suspend fun updateRemotes(block: suspend () -> Either<List<RemoteInfo>, AppError>)
    suspend fun updateStashes(block: suspend () -> Either<List<Commit>, AppError>)
    suspend fun updateSubmodules(block: suspend () -> Either<Map<String, Submodule>, AppError>)
    suspend fun updateAuthor(block: suspend () -> Either<AuthorInfo, AppError>)
    suspend fun updateRepositoryState(block: suspend () -> Either<RepositoryState, AppError>)
    suspend fun updateRebaseInteractiveState(block: suspend () -> Either<List<RebaseLine>, AppError>)
}

sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Loaded<T>(val data: T) : DataState<T>
    data class Error(val error: AppError) : DataState<Nothing>

}
