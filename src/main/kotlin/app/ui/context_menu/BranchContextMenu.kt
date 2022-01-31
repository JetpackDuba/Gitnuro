package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun branchContextMenuItems(
    isCurrentBranch: Boolean,
    isLocal: Boolean,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
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
    }
}