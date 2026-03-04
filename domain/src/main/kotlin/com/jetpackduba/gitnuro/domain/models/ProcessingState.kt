package com.jetpackduba.gitnuro.domain.models

sealed interface ProcessingState {
    data object None : ProcessingState
    data class Processing(
        val title: String,
        val subtitle: String,
        val isCancellable: Boolean,
    ) : ProcessingState
}