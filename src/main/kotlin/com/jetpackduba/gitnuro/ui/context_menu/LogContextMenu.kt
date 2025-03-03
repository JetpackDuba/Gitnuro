package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.branch
import com.jetpackduba.gitnuro.generated.resources.start
import com.jetpackduba.gitnuro.generated.resources.tag
import org.jetbrains.compose.resources.painterResource

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
        icon = { painterResource(Res.drawable.start) },
        onClick = onCheckoutCommit
    )
    addContextMenu(
        label = "Create branch",
        icon = { painterResource(Res.drawable.branch) },
        onClick = onCreateNewBranch
    )
    addContextMenu(
        label = "Create tag",
        icon = { painterResource(Res.drawable.tag) },
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
        icon = { painterResource(Res.drawable.revert) },
        onClick = onRevertCommit
    )
    addContextMenu(
        label = "Cherry-pick commit",
        onClick = onCherryPickCommit
    )
    addContextMenu(
        label = "Reset current branch to this commit",
        icon = { painterResource(Res.drawable.undo) },
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