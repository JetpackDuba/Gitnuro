package com.jetpackduba.gitnuro.domain.errors

sealed interface AppError

sealed interface GitError : AppError


data class GenericError(val message: String, val exception: Exception? = null) : GitError

sealed interface CreateBranchError : GitError {
    data class BranchAlreadyExists(val name: String): CreateBranchError
}

/**
 * Repository path for current tab is not set
 */
data object RepositoryPathNotSetError : GitError

/**
 * Errors reading information from a repository (such as branches, status, etc.)
 */
data class RepositoryReadError(val message: String) : GitError

data class HookRejectionError(val message: String): GitError

sealed interface StashChangesError: GitError {
    data object NoDataToStash: StashChangesError
}

sealed interface FSWatchError: AppError {

}

sealed interface OpenRepoError: AppError {
    data object DirectoryNotFoundError : OpenRepoError
    data object PathIsNotDirectory : OpenRepoError
    data object RepositoryNotFoundInPath : OpenRepoError
    data class RepositoryLoadFailed(val error: String) : OpenRepoError
}