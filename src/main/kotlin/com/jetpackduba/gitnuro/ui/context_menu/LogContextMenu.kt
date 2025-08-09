package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.jetpackduba.gitnuro.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

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
        composableLabel = { stringResource(Res.string.log_context_menu_checkout_commit) },
        icon = { painterResource(Res.drawable.start) },
        onClick = onCheckoutCommit
    )
    addContextMenu(
        composableLabel = { stringResource(Res.string.log_context_menu_create_branch) },
        icon = { painterResource(Res.drawable.branch) },
        onClick = onCreateNewBranch
    )
    addContextMenu(
        composableLabel = { stringResource(Res.string.log_context_menu_create_tag) },
        icon = { painterResource(Res.drawable.tag) },
        onClick = onCreateNewTag
    )

    add(ContextMenuElement.ContextSeparator)

    if (!isLastCommit) {
        addContextMenu(
            composableLabel = { stringResource(Res.string.log_context_menu_rebase_interactive) },
            onClick = onRebaseInteractive
        )
    }

    addContextMenu(
        composableLabel = { stringResource(Res.string.log_context_menu_revert_commit) },
        icon = { painterResource(Res.drawable.revert) },
        onClick = onRevertCommit
    )
    addContextMenu(
        composableLabel = { stringResource(Res.string.log_context_menu_cherry_pick_commit) },
        onClick = onCherryPickCommit
    )
    addContextMenu(
        composableLabel = { stringResource(Res.string.log_context_menu_reset_current_branch_to_commit) },
        icon = { painterResource(Res.drawable.undo) },
        onClick = onResetBranch
    )
}
