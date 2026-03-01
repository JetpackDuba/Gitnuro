package com.jetpackduba.gitnuro.git.actions

import com.jetpackduba.gitnuro.git.Action
import com.jetpackduba.gitnuro.TaskType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.rebase.RebaseBranchUseCase
import com.jetpackduba.gitnuro.ui.context_menu.copyBranchNameToClipboardAndGetNotification
import org.jetbrains.skiko.ClipboardManager
import javax.inject.Inject

class BranchCopyNameToClipboardAction @Inject constructor(
    private val tabState: TabState,
    private val clipboardManager: ClipboardManager,
) : IAction<Action.BranchCopyNameToClipboard> {

    override fun invoke(action: Action.BranchCopyNameToClipboard) {
        val branch = action.ref

        tabState.safeProcessing(
            refreshType = RefreshType.NONE,
            taskType = TaskType.UNSPECIFIED
        ) {
            copyBranchNameToClipboardAndGetNotification(
                branch,
                clipboardManager
            )
        }
    }
}