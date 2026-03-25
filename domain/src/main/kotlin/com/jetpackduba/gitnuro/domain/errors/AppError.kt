package com.jetpackduba.gitnuro.domain.errors

sealed interface AppError

sealed interface GitError : AppError


data class GenericError(val message: String) : GitError

/**
 * Errors reading information from a repository (such as branches, status, etc.)
 */
data object RepositoryReadError : GitError