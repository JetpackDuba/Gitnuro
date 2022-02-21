package app.ui.context_menu

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
fun logContextMenu(
    onCheckoutCommit: () -> Unit,
    onCreateNewBranch: () -> Unit,
    onCreateNewTag: () -> Unit,
    onRevertCommit: () -> Unit,
    onCherryPickCommit: () -> Unit,
    onResetBranch: () -> Unit,
) = listOf(
    ContextMenuItem(
        label = "Checkout commit",
        onClick = onCheckoutCommit
    ),
    ContextMenuItem(
        label = "Create branch",
        onClick = onCreateNewBranch
    ),
    ContextMenuItem(
        label = "Create tag",
        onClick = onCreateNewTag
    ),
    ContextMenuItem(
        label = "Revert commit",
        onClick = onRevertCommit
    ),
    ContextMenuItem(
        label = "Cherry-pick commit",
        onClick = onCherryPickCommit
    ),

    ContextMenuItem(
        label = "Reset current branch to this commit",
        onClick = onResetBranch
    )
)