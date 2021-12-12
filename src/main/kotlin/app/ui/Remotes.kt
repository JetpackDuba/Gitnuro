package app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.MAX_SIDE_PANEL_ITEMS_HEIGHT
import app.ui.components.ScrollableLazyColumn
import app.extensions.simpleVisibleName
import app.git.GitManager
import app.git.RemoteInfo
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.ui.components.entryHeight

@Composable
fun Remotes(gitManager: GitManager) {
    val remotes by gitManager.remotes.collectAsState()

    Column {
        SideMenuEntry("Remotes")

        val allBranches = remotes.map { it.branchesList }.flatten()
        val remotesHeight = (allBranches.count() + remotes.count()) * entryHeight
        val maxHeight = if(remotesHeight < MAX_SIDE_PANEL_ITEMS_HEIGHT)
            remotesHeight
        else
            MAX_SIDE_PANEL_ITEMS_HEIGHT

        Box(modifier = Modifier.heightIn(max = maxHeight.dp)) {
            ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(remotes) { remote ->
                    RemoteRow(
                        remote = remote,
                    )
                }
            }
        }
    }
}

@Composable
private fun RemoteRow(
    remote: RemoteInfo,
) {
    SideMenuSubentry(
        text = remote.remoteConfig.name,
        iconResourcePath = "cloud.svg",
    )

    val branches = remote.branchesList
    Column {
        branches.forEach { branch ->
            SideMenuSubentry(
                text = branch.simpleVisibleName,
                extraPadding = 8.dp,
                iconResourcePath = "branch.svg",
            )
        }
    }
}