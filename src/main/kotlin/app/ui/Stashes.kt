package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.maxSidePanelHeight
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.viewmodels.StashStatus
import app.viewmodels.StashesViewModel
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun Stashes(
    stashesViewModel: StashesViewModel,
    onStashSelected: (commit: RevCommit) -> Unit,
) {
    val stashStatusState = stashesViewModel.stashStatus.collectAsState()
    val stashStatus = stashStatusState.value

    val stashList = if (stashStatus is StashStatus.Loaded)
        stashStatus.stashes
    else
        listOf()

    val maxHeight = remember(stashList) { maxSidePanelHeight(stashList.count()) }

    Column {
        SideMenuEntry(
            text = "Stashes",
        )

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight.dp)
                .background(MaterialTheme.colors.background)
        ) {
            items(items = stashList) { stash ->
                StashRow(
                    stash = stash,
                    onClick = {
                        onStashSelected(stash)
                    }
                )
            }
        }
    }

}

@Composable
private fun StashRow(stash: RevCommit, onClick: () -> Unit) {
    SideMenuSubentry(
        text = stash.shortMessage,
        iconResourcePath = "stash.svg",
        onClick = onClick,
    )
}