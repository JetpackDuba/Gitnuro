package com.jetpackduba.gitnuro.domain.models

sealed interface RepositorySelectionState {
    data object Unknown : RepositorySelectionState
    data object None : RepositorySelectionState
    data class Opening(val path: String) : RepositorySelectionState
    data class Open(val path: String) : RepositorySelectionState
}