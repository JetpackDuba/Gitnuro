package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.domain.interfaces.IApplyStashGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteStashGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IPopStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.Job
import javax.inject.Inject

interface ISharedStashViewModel {
    fun applyStash(stashInfo: Commit): Job
    fun popStash(stash: Commit): Job
    fun deleteStash(stash: Commit): Job
    fun selectStash(stash: Commit): Job
    fun stashDropped(stash: Commit): Job
}

class SharedStashViewModel @Inject constructor(
    private val applyStashGitAction: IApplyStashGitAction,
    private val popStashGitAction: IPopStashGitAction,
    private val deleteStashGitAction: IDeleteStashGitAction,
    private val tabState: TabInstanceRepository,
) : ISharedStashViewModel {
    override fun applyStash(stashInfo: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
        taskType = TaskType.APPLY_STASH,
    ) { git ->
        applyStashGitAction(git, stashInfo)

        positiveNotification("Stash applied")
    }

    override fun popStash(stash: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
        taskType = TaskType.POP_STASH,
    ) { git ->
        popStashGitAction(git, stash)

        stashDropped(stash)

        positiveNotification("Stash popped")
    }

    override fun deleteStash(stash: Commit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
        taskType = TaskType.DELETE_STASH,
    ) { git ->
        deleteStashGitAction(git.repository.directory.absolutePath, stash)

        stashDropped(stash)

        positiveNotification("Stash deleted")
    }

    override fun selectStash(stash: Commit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        tabState.newSelectedStash(stash)
    }

    override fun stashDropped(stash: Commit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) {
        val selectedValue = tabState.selectedItem.value
        if (
            selectedValue is SelectedItem.Stash &&
            selectedValue.commit.hash == stash.hash
        ) {
            tabState.noneSelected()
        }
    }
}