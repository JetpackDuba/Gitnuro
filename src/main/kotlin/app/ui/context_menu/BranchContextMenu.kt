package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import app.extensions.simpleLogName
import org.eclipse.jgit.lib.Ref

@OptIn(ExperimentalFoundationApi::class)
fun branchContextMenuItems(
    branch: Ref,
    isCurrentBranch: Boolean,
    currentBranchName: String,
    isLocal: Boolean,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onPushToRemoteBranch: () -> Unit,
    onPullFromRemoteBranch: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf<ContextMenuItem>().apply {
        if (!isCurrentBranch) {
            add(
                ContextMenuItem(
                    label = "Checkout branch",
                    onClick = onCheckoutBranch
                )
            )
            add(
                ContextMenuItem(
                    label = "Merge branch",
                    onClick = onMergeBranch
                )
            )
            add(
                ContextMenuItem(
                    label = "Rebase branch",
                    onClick = onRebaseBranch
                )
            )
        }
        if (isLocal && !isCurrentBranch) {
            add(
                ContextMenuItem(
                    label = "Delete branch",
                    onClick = onDeleteBranch
                )
            )
        }
        if (!isLocal) {
            add(
                ContextMenuItem(
                    label = "Push $currentBranchName to ${branch.simpleLogName}",
                    onClick = onPushToRemoteBranch
                )
            )
            add(
                ContextMenuItem(
                    label = "Pull ${branch.simpleLogName} to $currentBranchName",
                    onClick = onPullFromRemoteBranch
                )
            )
        }
    }
}