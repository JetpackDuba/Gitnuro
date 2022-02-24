package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import app.extensions.isBranch
import app.extensions.isHead
import app.extensions.simpleLogName
import org.eclipse.jgit.lib.Ref

@OptIn(ExperimentalFoundationApi::class)
fun branchContextMenuItems(
    branch: Ref,
    isCurrentBranch: Boolean,
    currentBranch: Ref?,
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
            if(currentBranch != null && !currentBranch.isHead) {
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
        }
        if (isLocal && !isCurrentBranch) {
            add(
                ContextMenuItem(
                    label = "Delete branch",
                    onClick = onDeleteBranch
                )
            )
        }
        if (!isLocal && currentBranch != null && !currentBranch.isHead) {
            add(
                ContextMenuItem(
                    label = "Push ${currentBranch.simpleLogName} to ${branch.simpleLogName}",
                    onClick = onPushToRemoteBranch
                )
            )
            add(
                ContextMenuItem(
                    label = "Pull ${branch.simpleLogName} to ${currentBranch.simpleLogName}",
                    onClick = onPullFromRemoteBranch
                )
            )
        }
    }
}