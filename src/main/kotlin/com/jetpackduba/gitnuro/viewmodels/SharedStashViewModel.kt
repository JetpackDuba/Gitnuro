package com.jetpackduba.gitnuro.viewmodels

import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.stash.ApplyStashUseCase
import com.jetpackduba.gitnuro.git.stash.DeleteStashUseCase
import com.jetpackduba.gitnuro.git.stash.PopStashUseCase
import com.jetpackduba.gitnuro.ui.SelectedItem
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
    private val applyStashUseCase: ApplyStashUseCase,
    private val popStashUseCase: PopStashUseCase,
    private val deleteStashUseCase: DeleteStashUseCase,
    private val tabState: TabState,
) : ISharedStashViewModel {
    override fun applyStash(stashInfo: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
    ) { git ->
        applyStashUseCase(git, stashInfo)
    }

    override fun popStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.UNCOMMITED_CHANGES_AND_LOG,
        refreshEvenIfCrashes = true,
    ) { git ->
        popStashUseCase(git, stash)

        stashDropped(stash)
    }

    override fun deleteStash(stash: RevCommit) = tabState.safeProcessing(
        refreshType = RefreshType.STASHES,
    ) { git ->
        deleteStashUseCase(git, stash)

        stashDropped(stash)
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