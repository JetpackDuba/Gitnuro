package com.jetpackduba.gitnuro.git.rebase

sealed interface RebaseInteractiveState {
    object None : RebaseInteractiveState
    object AwaitingInteraction : RebaseInteractiveState
    data class ProcessingCommits(val commitToAmendId: String?) : RebaseInteractiveState {
        val isCurrentStepAmenable: Boolean = !commitToAmendId.isNullOrBlank()
    }
}