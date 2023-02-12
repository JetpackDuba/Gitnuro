package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource

fun logContextMenu(
    onCheckoutCommit: () -> Unit,
    onCreateNewBranch: () -> Unit,
    onCreateNewTag: () -> Unit,
    onRevertCommit: () -> Unit,
    onCherryPickCommit: () -> Unit,
    onResetBranch: () -> Unit,
    onRebaseInteractive: () -> Unit,
    showSquashCommits: Boolean,
    onSquashCommits: () -> Unit,
) = listOf(
    ContextMenuElement.ContextTextEntry(
        label = "Checkout commit",
        icon = { painterResource("start.svg") },
        onClick = onCheckoutCommit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Squash commits",
        icon = { painterResource("branch.svg") },
        isVisible = showSquashCommits,
        onClick = onSquashCommits
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Create branch",
        icon = { painterResource("branch.svg") },
        onClick = onCreateNewBranch
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Create tag",
        icon = { painterResource("tag.svg") },
        onClick = onCreateNewTag
    ),
    ContextMenuElement.ContextSeparator,
    ContextMenuElement.ContextTextEntry(
        label = "Rebase interactive",
        onClick = onRebaseInteractive
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Revert commit",
        icon = { painterResource("revert.svg") },
        onClick = onRevertCommit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Cherry-pick commit",
        onClick = onCherryPickCommit
    ),
    ContextMenuElement.ContextTextEntry(
        label = "Reset current branch to this commit",
        icon = { painterResource("undo.svg") },
        onClick = onResetBranch
    ),
)