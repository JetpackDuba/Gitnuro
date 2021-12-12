package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.MAX_SIDE_PANEL_ITEMS_HEIGHT
import app.extensions.isLocal
import app.extensions.simpleName
import app.git.GitManager
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.ui.components.entryHeight
import app.ui.context_menu.branchContextMenuItems
import app.ui.dialogs.MergeDialog
import org.eclipse.jgit.lib.Ref

@Composable
fun Branches(
    gitManager: GitManager,
    onBranchClicked: (Ref) -> Unit,

    ) {
    val branches by gitManager.branches.collectAsState()
    val currentBranch by gitManager.currentBranch.collectAsState()
    val (mergeBranch, setMergeBranch) = remember { mutableStateOf<Ref?>(null) }

    Column {
        SideMenuEntry("Local branches")

        val branchesHeight = branches.count() * entryHeight
        val maxHeight = if (branchesHeight < MAX_SIDE_PANEL_ITEMS_HEIGHT)
            branchesHeight
        else
            MAX_SIDE_PANEL_ITEMS_HEIGHT

        Box(modifier = Modifier.heightIn(max = maxHeight.dp)) {
            ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(branches) { _, branch ->
                    BranchRow(
                        branch = branch,
                        isCurrentBranch = currentBranch == branch.name,
                        onBranchClicked = { onBranchClicked(branch) },
                        onCheckoutBranch = { gitManager.checkoutRef(branch) },
                        onMergeBranch = { setMergeBranch(branch) },
                        onDeleteBranch = { gitManager.deleteBranch(branch) },
                    )
                }
            }
        }
    }

    if (mergeBranch != null) {
        MergeDialog(
            currentBranch,
            mergeBranchName = mergeBranch.name,
            onReject = { setMergeBranch(null) },
            onAccept = { ff -> gitManager.mergeBranch(mergeBranch, ff) }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BranchRow(
    branch: Ref,
    isCurrentBranch: Boolean,
    onBranchClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
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