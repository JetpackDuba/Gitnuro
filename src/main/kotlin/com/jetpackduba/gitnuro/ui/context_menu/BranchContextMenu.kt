package com.jetpackduba.gitnuro.ui.context_menu

import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.isHead
import com.jetpackduba.gitnuro.extensions.simpleLogName
import org.eclipse.jgit.lib.Ref

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
): List<ContextMenuElement> {
    return mutableListOf<ContextMenuElement>().apply {
        if (!isCurrentBranch) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Checkout branch",
                    icon = { painterResource(AppIcons.START) },
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
                    icon = { painterResource(AppIcons.DELETE) },
                    onClick = onDeleteRemoteBranch
                ),
            )
        }

        if (isLocal && !isCurrentBranch) {
            add(
                ContextMenuElement.ContextTextEntry(
                    label = "Delete branch",
                    icon = { painterResource(AppIcons.DELETE) },
                    onClick = onDeleteBranch
                )
            )
        }

        if (lastOrNull() == ContextMenuElement.ContextSeparator) {
            removeLast()
        }
    }
}