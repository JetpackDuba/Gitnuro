package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.git.actions.BranchCopyNameToClipboardAction
import com.jetpackduba.gitnuro.git.actions.BranchDeleteAction
import com.jetpackduba.gitnuro.git.actions.BranchMergeAction
import com.jetpackduba.gitnuro.git.actions.BranchRebaseAction
import com.jetpackduba.gitnuro.git.actions.RefCheckoutAction
import jakarta.inject.Inject
import org.eclipse.jgit.lib.Ref

sealed interface Action {
    data class BranchMerge(val ref: Ref) : Action
    data class BranchDelete(val ref: Ref) : Action
    data class BranchRebase(val ref: Ref) : Action
    data class BranchCopyNameToClipboard(val ref: Ref) : Action
    data class RefCheckout(val ref: Ref) : Action
}

@TabScope
class ActionsExecutor @Inject constructor(
    private val branchMergeAction: BranchMergeAction,
    private val branchDeleteAction: BranchDeleteAction,
    private val branchRebaseAction: BranchRebaseAction,
    private val branchCopyNameToClipboardAction: BranchCopyNameToClipboardAction,
    private val refCheckoutAction: RefCheckoutAction,
) {
    fun runAction(action: Action) {
        when (action) {
            is Action.BranchDelete -> branchDeleteAction(action)
            is Action.BranchMerge -> branchMergeAction(action)
            is Action.BranchCopyNameToClipboard -> branchCopyNameToClipboardAction(action)
            is Action.BranchRebase -> branchRebaseAction(action)
            is Action.RefCheckout -> refCheckoutAction(action)
        }
    }
}