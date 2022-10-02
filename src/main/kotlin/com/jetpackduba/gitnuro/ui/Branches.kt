package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.isLocal
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.theme.secondaryTextColor
import com.jetpackduba.gitnuro.ui.components.SideMenuPanel
import com.jetpackduba.gitnuro.ui.components.SideMenuSubentry
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.branchContextMenuItems
import com.jetpackduba.gitnuro.viewmodels.BranchesViewModel
import org.eclipse.jgit.lib.Ref

@Composable
fun Branches(
    branchesViewModel: BranchesViewModel,
) {
    val branches by branchesViewModel.branches.collectAsState()
    val currentBranchState = branchesViewModel.currentBranch.collectAsState()
    val isExpanded by branchesViewModel.isExpanded.collectAsState()
    val currentBranch = currentBranchState.value

    SideMenuPanel(
        title = "Local branches",
        icon = painterResource("branch.svg"),
        items = branches,
        isExpanded = isExpanded,
        onExpand = { branchesViewModel.onExpand() },
        itemContent = { branch ->
            BranchLineEntry(
                branch = branch,
                currentBranch = currentBranch,
                isCurrentBranch = currentBranch?.name == branch.name,
                onBranchClicked = { branchesViewModel.selectBranch(branch) },
                onBranchDoubleClicked = { branchesViewModel.checkoutRef(branch) },
                onCheckoutBranch = { branchesViewModel.checkoutRef(branch) },
                onMergeBranch = { branchesViewModel.mergeBranch(branch) },
                onDeleteBranch = { branchesViewModel.deleteBranch(branch) },
                onRebaseBranch = { branchesViewModel.rebaseBranch(branch) },
                onPushToRemoteBranch = { branchesViewModel.pushToRemoteBranch(branch) },
                onPullFromRemoteBranch = { branchesViewModel.pullFromRemoteBranch(branch) },
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BranchLineEntry(
    branch: Ref,
    currentBranch: Ref?,
    isCurrentBranch: Boolean,
    onBranchClicked: () -> Unit,
    onBranchDoubleClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onPushToRemoteBranch: () -> Unit,
    onPullFromRemoteBranch: () -> Unit,
) {
    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = branch,
                currentBranch = currentBranch,
                isCurrentBranch = isCurrentBranch,
                isLocal = branch.isLocal,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeBranch,
                onDeleteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseBranch,
                onPushToRemoteBranch = onPushToRemoteBranch,
                onPullFromRemoteBranch = onPullFromRemoteBranch,
            )
        }
    ) {
        SideMenuSubentry(
            text = branch.simpleName,
            iconResourcePath = "branch.svg",
            onClick = onBranchClicked,
            onDoubleClick = onBranchDoubleClicked,
        ) {
            if (isCurrentBranch) {
                Text(
                    text = "HEAD",
                    color = MaterialTheme.colors.secondaryTextColor,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}