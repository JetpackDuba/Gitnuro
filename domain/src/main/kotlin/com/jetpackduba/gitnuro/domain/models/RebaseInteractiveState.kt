package com.jetpackduba.gitnuro.domain.models

sealed interface RebaseInteractiveState {
    data object None : RebaseInteractiveState
    data object AwaitingInteraction : RebaseInteractiveState
    data class ProcessingCommits(val commitToAmendId: String?) : RebaseInteractiveState {
        val isCurrentStepAmenable: Boolean = !commitToAmendId.isNullOrBlank()
    }
}