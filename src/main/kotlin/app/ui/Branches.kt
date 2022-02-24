package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.extensions.isLocal
import app.extensions.simpleName
import app.ui.components.SideMenuPanel
import app.ui.components.SideMenuSubentry
import app.ui.context_menu.branchContextMenuItems
import app.ui.dialogs.MergeDialog
import app.ui.dialogs.RebaseDialog
import app.viewmodels.BranchesViewModel
import org.eclipse.jgit.lib.Ref

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Branches(
    branchesViewModel: BranchesViewModel,
) {
    val branches by branchesViewModel.branches.collectAsState()
    val currentBranchState = branchesViewModel.currentBranch.collectAsState()
    val currentBranch = currentBranchState.value

    val (mergeBranch, setMergeBranch) = remember { mutableStateOf<Ref?>(null) }
    val (rebaseBranch, setRebaseBranch) = remember { mutableStateOf<Ref?>(null) }

    SideMenuPanel(
        title = "Local branches",
        icon = painterResource("branch.svg"),
        items = branches,
        itemContent = { branch ->
            BranchLineEntry(
                branch = branch,
                currentBranch = currentBranch,
                isCurrentBranch = currentBranch?.name == branch.name,
                onBranchClicked = { branchesViewModel.selectBranch(branch) },
                onCheckoutBranch = { branchesViewModel.checkoutRef(branch) },
                onMergeBranch = { setMergeBranch(branch) },
                onDeleteBranch = { branchesViewModel.deleteBranch(branch) },
                onRebaseBranch = { setRebaseBranch(branch) },
                onPushToRemoteBranch = { branchesViewModel.pushToRemoteBranch(branch) },
                onPullFromRemoteBranch = { branchesViewModel.pullFromRemoteBranch(branch) },
            )
        }
    )

    if (mergeBranch != null && currentBranch != null) {
        MergeDialog(
            currentBranchName = currentBranch.simpleName,
            mergeBranchName = mergeBranch.name,
            onReject = { setMergeBranch(null) },
            onAccept = { ff -> branchesViewModel.mergeBranch(mergeBranch, ff) }
        )
    }

    if (rebaseBranch != null && currentBranch != null) {
        RebaseDialog(
            currentBranchName = currentBranch.simpleName,
            rebaseBranchName = rebaseBranch.name,
            onReject = { setRebaseBranch(null) },
            onAccept = { branchesViewModel.rebaseBranch(rebaseBranch) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BranchLineEntry(
    branch: Ref,
    currentBranch: Ref?,
    isCurrentBranch: Boolean,
    onBranchClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onPushToRemoteBranch: () -> Unit,
    onPullFromRemoteBranch: () -> Unit,
) {
    ContextMenuArea(
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
            bold = isCurrentBranch,
            onClick = onBranchClicked
        ) {
            if (isCurrentBranch) {
                Icon(
                    painter = painterResource("location.svg"),
                    contentDescription = null,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }
}