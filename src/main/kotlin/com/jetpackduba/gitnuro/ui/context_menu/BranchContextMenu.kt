package com.jetpackduba.gitnuro.ui.context_menu

import org.jetbrains.compose.resources.painterResource
import com.jetpackduba.gitnuro.extensions.isHead
import com.jetpackduba.gitnuro.extensions.simpleLogName
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.copy
import com.jetpackduba.gitnuro.generated.resources.delete
import com.jetpackduba.gitnuro.generated.resources.start
import com.jetpackduba.gitnuro.models.Notification
import com.jetpackduba.gitnuro.models.positiveNotification
import org.eclipse.jgit.lib.Ref
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
    onCopyBranchNameToClipboard: () -> Unit
): List<ContextMenuElement> {

    return mutableListOf<ContextMenuElement>().apply {
        if (!isCurrentBranch) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Checkout branch",
                    icon = { painterResource(Res.drawable.start) },
                    onClick = onCheckoutBranch
                )
            )
            if (currentBranch != null && !currentBranch.isHead) {
                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "Merge branch",
                        onClick = onMergeBranch
                    )
                )
                add(
                    ContextMenuElement.ContextTextEntry(
                        label = "Rebase branch",
                        onClick = onRebaseBranch
                    )
                )

                add(ContextMenuElement.ContextSeparator)
            }
        }
        if (!isLocal && currentBranch != null && !currentBranch.isHead) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Push ${currentBranch.simpleLogName} to ${branch.simpleLogName}",
                    onClick = onPushToRemoteBranch
                )
            )
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Pull ${branch.simpleLogName} to ${currentBranch.simpleLogName}",
                    onClick = onPullFromRemoteBranch
                )
            )
        }

        if (isLocal) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Change default upstream branch",
                    onClick = onChangeDefaultUpstreamBranch
                ),
            )
        }

        if (!isLocal) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Delete remote branch",
                    icon = { painterResource(Res.drawable.delete) },
                    onClick = onDeleteRemoteBranch
                ),
            )
        }

        if (isLocal && !isCurrentBranch) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Delete branch",
                    icon = { painterResource(Res.drawable.delete) },
                    onClick = onDeleteBranch
                )
            )
        }

        add(
            ContextMenuElement.ContextTextEntry(
                label = "Copy branch name",
                icon = { painterResource(Res.drawable.copy) },
                onClick = {
                    onCopyBranchNameToClipboard()
                }
            )
        )
    }
}

internal fun copyBranchNameToClipboardAndGetNotification(
    branch: Ref,
    clipboardManager: ClipboardManager
): Notification {
    val branchName = branch.simpleName
    clipboardManager.setText(branchName)
    return positiveNotification("\"${branchName}\" copied to clipboard")
}