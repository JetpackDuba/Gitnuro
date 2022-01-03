package app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import app.git.TabViewModel
import app.git.StashStatus
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun Stashes(
    gitManager: TabViewModel,
    onStashSelected: (commit: RevCommit) -> Unit,
) {
    val stashStatusState = gitManager.stashStatus.collectAsState()
    val stashStatus = stashStatusState.value

    val stashList = if (stashStatus is StashStatus.Loaded)
        stashStatus.stashes
    else
        listOf()


    Column {
        SideMenuEntry(
            text = "Stashes",
        )

        ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
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