package com.jetpackduba.gitnuro.domain.errors

sealed interface AppError

sealed interface GitError : AppError


data class GenericError(val message: String) : GitError

/**
 * Repository path for current tab is not set
 */
data object RepositoryPathNotSetError : GitError

/**
 * Errors reading information from a repository (such as branches, status, etc.)
 */
data object RepositoryReadError : GitError

data class HookRejectionError(val message: String): GitError