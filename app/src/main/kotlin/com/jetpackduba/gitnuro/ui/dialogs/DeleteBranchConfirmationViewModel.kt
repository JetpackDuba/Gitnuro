package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.usecases.DeleteBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.DeleteRemoteBranchUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DeleteBranchConfirmationViewModel @AssistedInject constructor(
    private val deleteBranchUseCase: DeleteBranchUseCase,
    private val deleteRemoteBranchUseCase: DeleteRemoteBranchUseCase,
    @Assisted val targetBranch: Branch,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(branch: Branch): DeleteBranchConfirmationViewModel
    }

    fun deleteBranch() {
        if (targetBranch.isLocal) {
            deleteBranchUseCase(targetBranch)
        } else {
            deleteRemoteBranchUseCase(targetBranch)
        }
    }
}