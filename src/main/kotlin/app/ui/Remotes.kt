package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.extensions.simpleVisibleName
import app.git.RemoteInfo
import app.maxSidePanelHeight
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.viewmodels.RemotesViewModel

@Composable
fun Remotes(remotesViewModel: RemotesViewModel) {
    val remotes by remotesViewModel.remotes.collectAsState()
    val allBranches = remotes.map { it.branchesList }.flatten()
    val maxHeight = remember(remotes) { maxSidePanelHeight(allBranches.count() + remotes.count()) }

    Column {
        SideMenuEntry("Remotes")

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight.dp)
                .background(MaterialTheme.colors.background)
        ) {
            items(remotes) { remote ->
                RemoteRow(
                    remote = remote,
                )
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