package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons

fun logContextMenu(
    onCheckoutCommit: () -> Unit,
    onCreateNewBranch: () -> Unit,
    onCreateNewTag: () -> Unit,
    onRevertCommit: () -> Unit,
    onCherryPickCommit: () -> Unit,
    onResetBranch: () -> Unit,
    onRebaseInteractive: () -> Unit,
) = listOf(
    ContextMenuElement.ContextTextEntry(
        label = "Checkout commit",
        icon = { painterResource(AppIcons.START) },
        onClick = onCheckoutCommit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Create branch",
        icon = { painterResource(AppIcons.BRANCH) },
        onClick = onCreateNewBranch
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Create tag",
        icon = { painterResource(AppIcons.TAG) },
        onClick = onCreateNewTag
    ),
    ContextMenuElement.ContextSeparator,
    ContextMenuElement.ContextTextEntry(
        label = "Rebase interactive",
        onClick = onRebaseInteractive
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Revert commit",
        icon = { painterResource(AppIcons.REVERT) },
        onClick = onRevertCommit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Cherry-pick commit",
        onClick = onCherryPickCommit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Reset current branch to this commit",
        icon = { painterResource(AppIcons.UNDO) },
        onClick = onResetBranch
    ),
)