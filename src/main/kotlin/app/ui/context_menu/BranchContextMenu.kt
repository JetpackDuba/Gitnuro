package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun branchContextMenuItems(
    isCurrentBranch: Boolean,
    isLocal: Boolean,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
): List<ContextMenuItem> {
    return mutableListOf(
        ContextMenuItem(
            label = "Checkout branch",
            onClick = onCheckoutBranch
        ),

        ).apply {
        if (!isCurrentBranch) {
            add(
                ContextMenuItem(
                    label = "Merge branch",
                    onClick = onMergeBranch
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