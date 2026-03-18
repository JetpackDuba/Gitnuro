package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Notification
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skiko.ClipboardManager

fun branchContextMenuItems(
    branch: Branch,
    isCurrentBranch: Boolean,
    currentBranch: Branch?,
    isLocal: Boolean,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onDeleteRemoteBranch: () -> Unit = {},
    onPushToRemoteBranch: () -> Unit,
    onPullFromRemoteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
    onRenameBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit,
): List<ContextMenuElement> {

    return mutableListOf<ContextMenuElement>().apply {
        if (!isCurrentBranch) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.branch_context_menu_checkout_branch) },
                icon = { painterResource(Res.drawable.start) },
                onClick = onCheckoutBranch
            )
            if (currentBranch != null && currentBranch.name != "HEAD") {
                addContextMenu(
                    composableLabel = { stringResource(Res.string.branch_context_menu_merge_branch) },
                    onClick = onMergeBranch
                )
                addContextMenu(
                    composableLabel = { stringResource(Res.string.branch_context_menu_rebase_branch) },
                    onClick = onRebaseBranch
                )

                add(ContextMenuElement.ContextSeparator)
            }
        }
        if (!isLocal && currentBranch != null && currentBranch.name != "HEAD") { // TODO currentBranch.name != "HEAD" as function or even extension function?
            addContextMenu(
                composableLabel = {
                    stringResource(
                        Res.string.branch_context_menu_push_current_to_target,
                        currentBranch.name,
                        branch.name,
                    )
                },
                onClick = onPushToRemoteBranch
            )
            addContextMenu(
                composableLabel = {
                    stringResource(
                        Res.string.branch_context_menu_pull_target_to_current,
                        branch.name,
                        currentBranch.name,
                    )
                },
                onClick = onPullFromRemoteBranch,
            )

            add(ContextMenuElement.ContextSeparator)
        }

        if (isLocal) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.branch_context_menu_rename_branch) },
                icon = { painterResource(Res.drawable.edit) },
                onClick = onRenameBranch,
            )

            addContextMenu(
                composableLabel = { stringResource(Res.string.branch_context_menu_change_default_upstream_branch) },
                onClick = onChangeDefaultUpstreamBranch
            )

            add(ContextMenuElement.ContextSeparator)
        }

        if (!isLocal) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.branch_context_menu_delete_remote_branch) },
                icon = { painterResource(Res.drawable.delete) },
                onClick = onDeleteRemoteBranch,
            )

            add(ContextMenuElement.ContextSeparator)
        }

        if (isLocal && !isCurrentBranch) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.branch_context_menu_delete_branch) },
                icon = { painterResource(Res.drawable.delete) },
                onClick = onDeleteBranch,
            )

            add(ContextMenuElement.ContextSeparator)
        }

        addContextMenu(
            composableLabel = { stringResource(Res.string.branch_context_menu_copy_branch_name) },
            icon = { painterResource(Res.drawable.copy) },
            onClick = {
                onCopyBranchNameToClipboard()
            }
        )
    }
}

internal suspend fun copyBranchNameToClipboardAndGetNotification(
    branch: Branch,
    clipboardManager: ClipboardManager,
): Notification {
    val branchName = branch.simpleName
    clipboardManager.setText(branchName)
    return positiveNotification(getString(Res.string.notification_copied_branch_to_clipboard, branchName))
}