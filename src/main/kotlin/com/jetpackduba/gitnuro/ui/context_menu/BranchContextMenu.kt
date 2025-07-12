package com.jetpackduba.gitnuro.ui.context_menu

import com.jetpackduba.gitnuro.extensions.isHead
import com.jetpackduba.gitnuro.extensions.simpleLogName
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.models.Notification
import com.jetpackduba.gitnuro.models.positiveNotification
import org.eclipse.jgit.lib.Ref
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skiko.ClipboardManager

fun branchContextMenuItems(
    branch: Ref,
    isCurrentBranch: Boolean,
    currentBranch: Ref?,
    isLocal: Boolean,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onDeleteRemoteBranch: () -> Unit = {},
    onPushToRemoteBranch: () -> Unit,
    onPullFromRemoteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit,
): List<ContextMenuElement> {

    return mutableListOf<ContextMenuElement>().apply {
        if (!isCurrentBranch) {
            addContextMenu(
                composableLabel = { stringResource(Res.string.branch_context_menu_checkout_branch) },
                icon = { painterResource(Res.drawable.start) },
                onClick = onCheckoutBranch
            )
            if (currentBranch != null && !currentBranch.isHead) {
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
        if (!isLocal && currentBranch != null && !currentBranch.isHead) {
            addContextMenu(
                composableLabel = {
                    stringResource(
                        Res.string.branch_context_menu_push_current_to_target,
                        currentBranch.simpleLogName,
                        branch.simpleLogName,
                    )
                },
                onClick = onPushToRemoteBranch
            )
            addContextMenu(
                composableLabel = {
                    stringResource(
                        Res.string.branch_context_menu_pull_target_to_current,
                        branch.simpleLogName,
                        currentBranch.simpleLogName,
                    )
                },
                onClick = onPullFromRemoteBranch,
            )

            add(ContextMenuElement.ContextSeparator)
        }

        if (isLocal) {
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

internal fun copyBranchNameToClipboardAndGetNotification(
    branch: Ref,
    clipboardManager: ClipboardManager,
): Notification {
    val branchName = branch.simpleName
    clipboardManager.setText(branchName)
    return positiveNotification("\"${branchName}\" copied to clipboard")
}