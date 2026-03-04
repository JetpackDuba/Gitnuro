package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.git.branches.RenameBranchGitAction
import com.jetpackduba.gitnuro.domain.git.branches.SetTrackingBranchGitAction
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class RenameBranchDialogViewModel @AssistedInject constructor(
    private val tabState: TabInstanceRepository,
    private val renameBranchGitAction: RenameBranchGitAction,
    private val setTrackingBranchGitAction: SetTrackingBranchGitAction,
    @Assisted val branch: Ref,
) : TabViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(branch: Ref): RenameBranchDialogViewModel
    }

    private val _operationCompleted = MutableStateFlow(false)
    val operationCompleted = _operationCompleted.asStateFlow()

    fun renameBranch(branch: Ref, newName: String) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.RENAME_BRANCH,
    ) { git ->

        val newRef = renameBranchGitAction(git, branch.name, newName)

        if (newRef != null) {
            setTrackingBranchGitAction(git, newRef, null, null)
        }
        _operationCompleted.value = true

        positiveNotification("Branch renamed")
    }
}
