package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.MAX_SIDE_PANEL_ITEMS_HEIGHT
import app.extensions.isLocal
import app.ui.components.ScrollableLazyColumn
import app.extensions.simpleName
import app.git.GitManager
import org.eclipse.jgit.lib.Ref
import app.theme.headerBackground
import app.theme.headerText
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.ui.components.entryHeight
import app.ui.context_menu.branchContextMenuItems
import app.ui.dialogs.MergeDialog

@Composable
fun Branches(gitManager: GitManager) {
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
                        onCheckoutBranch = { gitManager.checkoutRef(branch) },
                        onMergeBranch = { setMergeBranch(branch) },
                        onDeleteBranch = { gitManager.deleteBranch(branch) },
                    )
                }
            }
        }
    }

    if(mergeBranch != null) {
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