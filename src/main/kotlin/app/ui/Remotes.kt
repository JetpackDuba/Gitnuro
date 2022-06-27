@file:OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)

package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.extensions.simpleName
import app.theme.primaryTextColor
import app.ui.components.SideMenuPanel
import app.ui.components.SideMenuSubentry
import app.ui.components.VerticalExpandable
import app.ui.context_menu.remoteBranchesContextMenu
import app.ui.context_menu.remoteContextMenu
import app.ui.dialogs.EditRemotesDialog
import app.viewmodels.RemoteView
import app.viewmodels.RemotesViewModel
import org.eclipse.jgit.lib.Ref

@Composable
fun Remotes(
    remotesViewModel: RemotesViewModel,
) {
    val remotes by remotesViewModel.remotes.collectAsState()
    var showEditRemotesDialog by remember { mutableStateOf(false) }
    val isExpanded by remotesViewModel.isExpanded.collectAsState()

    if (showEditRemotesDialog) {
        EditRemotesDialog(
            remotesViewModel = remotesViewModel,
            onDismiss = {
                showEditRemotesDialog = false
            },
        )
    }

    SideMenuPanel(
        title = "Remotes",
        icon = painterResource("cloud.svg"),
        items = remotes,
        isExpanded = isExpanded,
        onExpand = { remotesViewModel.onExpand() },
        contextItems = {
            remoteContextMenu { showEditRemotesDialog = true }
        },
        headerHoverIcon = {
            IconButton(
                onClick = { showEditRemotesDialog = true },
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(16.dp)
                    .pointerHoverIcon(PointerIconDefaults.Hand),
            ) {
                Icon(
                    painter = painterResource("settings.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize(),
                    tint = MaterialTheme.colors.primaryTextColor,
                )
            }
        },
        itemContent = { remoteInfo ->
            RemoteRow(
                remote = remoteInfo,
                onBranchClicked = { branch -> remotesViewModel.selectBranch(branch) },
                onDeleteBranch = { branch -> remotesViewModel.deleteRemoteBranch(branch) },
                onRemoteClicked = { remotesViewModel.onRemoteClicked(remoteInfo) }
            )
        },
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RemoteRow(
    remote: RemoteView,
    onRemoteClicked: () -> Unit,
    onBranchClicked: (Ref) -> Unit,
    onDeleteBranch: (Ref) -> Unit,
) {
    VerticalExpandable(
        isExpanded = remote.isExpanded,
        onExpand = onRemoteClicked,
        header = {
            SideMenuSubentry(
                text = remote.remoteInfo.remoteConfig.name,
                iconResourcePath = "cloud.svg",
            )
        }
    ) {
        val branches = remote.remoteInfo.branchesList
        Column {
            branches.forEach { branch ->
                ContextMenuArea(
                    items = {
                        remoteBranchesContextMenu(
                            onDeleteBranch = { onDeleteBranch(branch) }
                        )
                    }
                ) {
                    SideMenuSubentry(
                        text = branch.simpleName,
                        extraPadding = 8.dp,
                        iconResourcePath = "branch.svg",
                        onClick = { onBranchClicked(branch) }
                    )
                }
            }
        }
    }
}