package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.extensions.isLocal
import app.extensions.simpleName
import app.maxSidePanelHeight
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.ui.context_menu.branchContextMenuItems
import app.ui.dialogs.MergeDialog
import app.ui.dialogs.RebaseDialog
import app.viewmodels.BranchesViewModel
import org.eclipse.jgit.lib.Ref

@Composable
fun Branches(
    branchesViewModel: BranchesViewModel,
    onBranchClicked: (Ref) -> Unit,
) {
    val branches by branchesViewModel.branches.collectAsState()
    val currentBranch by branchesViewModel.currentBranch.collectAsState()
    val (mergeBranch, setMergeBranch) = remember { mutableStateOf<Ref?>(null) }
    val (rebaseBranch, setRebaseBranch) = remember { mutableStateOf<Ref?>(null) }
    val maxHeight = remember(branches) { maxSidePanelHeight(branches.count()) }

    Column {
        SideMenuEntry("Local branches")

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight.dp)
                .background(MaterialTheme.colors.background)
        ) {
            itemsIndexed(branches) { _, branch ->
                BranchLineEntry(
                    branch = branch,
                    isCurrentBranch = currentBranch == branch.name,
                    onBranchClicked = { onBranchClicked(branch) },
                    onCheckoutBranch = { branchesViewModel.checkoutRef(branch) },
                    onMergeBranch = { setMergeBranch(branch) },
                    onRebaseBranch = { branchesViewModel.deleteBranch(branch) },
                    onDeleteBranch = { setRebaseBranch(branch) },
                )
            }
        }
    }

    if (mergeBranch != null) {
        MergeDialog(
            currentBranch,
            mergeBranchName = mergeBranch.name,
            onReject = { setMergeBranch(null) },
            onAccept = { ff -> branchesViewModel.mergeBranch(mergeBranch, ff) }
        )
    }

    if (rebaseBranch != null) {
        RebaseDialog(
            currentBranch,
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
    isCurrentBranch: Boolean,
    onBranchClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
) {
    ContextMenuArea(
        items = {
            branchContextMenuItems(
                isCurrentBranch = isCurrentBranch,
                isLocal = branch.isLocal,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeBranch,
                onDeleteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseBranch,
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