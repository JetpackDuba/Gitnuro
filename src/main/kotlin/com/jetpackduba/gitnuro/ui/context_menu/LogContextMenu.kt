package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
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
    isLastCommit: Boolean,
) = mutableListOf<ContextMenuElement>().apply {
    addContextMenu(
        label = "Checkout commit",
        icon = { painterResource(AppIcons.START) },
        onClick = onCheckoutCommit
    )
    addContextMenu(
        label = "Create branch",
        icon = { painterResource(AppIcons.BRANCH) },
        onClick = onCreateNewBranch
    )
    addContextMenu(
        label = "Create tag",
        icon = { painterResource(AppIcons.TAG) },
        onClick = onCreateNewTag
    )

    add(ContextMenuElement.ContextSeparator)

    if (!isLastCommit) {
        addContextMenu(
            label = "Rebase interactive",
            onClick = onRebaseInteractive
        )
    }

    addContextMenu(
        label = "Revert commit",
        icon = { painterResource(AppIcons.REVERT) },
        onClick = onRevertCommit
    )
    addContextMenu(
        label = "Cherry-pick commit",
        onClick = onCherryPickCommit
    )
    addContextMenu(
        label = "Reset current branch to this commit",
        icon = { painterResource(AppIcons.UNDO) },
        onClick = onResetBranch
    )
}

fun MutableList<ContextMenuElement>.addContextMenu(
    label: String,
    icon: @Composable (() -> Painter)? = null,
    onClick: () -> Unit = {}
) {
    this.add(
        ContextMenuElement.ContextTextEntry(
            label,
            icon,
            onClick,
        )
    )
}