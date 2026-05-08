package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.usecases.RenameBranchUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RenameBranchDialogViewModel @AssistedInject constructor(
    private val renameBranchUseCase: RenameBranchUseCase,
    @Assisted val branch: Branch,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(branch: Branch): RenameBranchDialogViewModel
    }

    val renameState: StateFlow<RenameState>
        field = MutableStateFlow<RenameState>(RenameState.Waiting)

    fun renameBranch(branch: Branch, newName: String) {
        if (renameState.value == RenameState.Renaming || renameState.value == RenameState.Success) {
            return
        }

        viewModelScope.launch {
            renameState.value = RenameState.Renaming

            val result = renameBranchUseCase(branch.name, newName)

            renameState.value = when (result) {
                is Either.Err -> RenameState.Failed(result.error)
                is Either.Ok -> RenameState.Success
            }
        }
    }
}

sealed interface RenameState {
    data object Waiting : RenameState
    data object Renaming : RenameState
    data object Success : RenameState
    data class Failed(val error: AppError) : RenameState
}