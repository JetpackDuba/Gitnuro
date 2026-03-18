package com.jetpackduba.gitnuro.ui.dialogs

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.usecases.CreateBranchUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.eclipse.jgit.revwalk.RevCommit


class CreateBranchViewModel @AssistedInject constructor(
    private val createBranchUseCase: CreateBranchUseCase,
    @Assisted private val commit: Commit?,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(commit: Commit?): CreateBranchViewModel
    }

    fun createBranch(branchName: String) {
        createBranchUseCase(branchName, commit)
    }
}