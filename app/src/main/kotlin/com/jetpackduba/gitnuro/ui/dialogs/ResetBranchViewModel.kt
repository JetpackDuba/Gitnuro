package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.usecases.ResetBranchUseCase
import com.jetpackduba.gitnuro.domain.usecases.ResetType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit

class ResetBranchViewModel @AssistedInject constructor(
    private val resetBranchUseCase: ResetBranchUseCase,
    @Assisted private val targetCommit: Commit,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: Commit): ResetBranchViewModel
    }

    fun reset(resetType: ResetType) {
        resetBranchUseCase(targetCommit, resetType)
    }
}