package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.git.stash.ApplyStashGitAction
import com.jetpackduba.gitnuro.domain.git.stash.DeleteStashGitAction
import com.jetpackduba.gitnuro.domain.git.stash.PopStashGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.Job
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

interface ISharedStashViewModel {
    fun applyStash(stashInfo: RevCommit): Job
    fun popStash(stash: RevCommit): Job
    fun deleteStash(stash: RevCommit): Job
    fun selectStash(stash: RevCommit): Job
    fun stashDropped(stash: RevCommit): Job
}

class SharedStashViewModel @Inject constructor(
    private val applyStashGitAction: ApplyStashGitAction,
    private val popStashGitAction: PopStashGitAction,
    private val deleteStashGitAction: DeleteStashGitAction,
    private val tabState: TabInstanceRepository,
) : ISharedStashViewModel {
    override fun applyStash(stashInfo: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
        taskType = TaskType.APPLY_STASH,
    ) { git ->
        applyStashGitAction(git, stashInfo)

        positiveNotification("Stash applied")
    }

    override fun popStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
        taskType = TaskType.POP_STASH,
    ) { git ->
        popStashGitAction(git, stash)

        stashDropped(stash)

        positiveNotification("Stash popped")
    }

    override fun deleteStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.DELETE_STASH,
    ) { git ->
        deleteStashGitAction(git, stash)

        stashDropped(stash)

        positiveNotification("Stash deleted")
    }

    override fun selectStash(stash: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedStash(stash)
    }

    override fun stashDropped(stash: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        val selectedValue = tabState.selectedItem.value
        if (
            selectedValue is SelectedItem.Stash &&
            selectedValue.revCommit.name == stash.name
        ) {
            tabState.noneSelected()
        }
    }
}