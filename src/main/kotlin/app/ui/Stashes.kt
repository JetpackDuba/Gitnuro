package app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.painterResource
import app.ui.components.SideMenuPanel
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


    SideMenuPanel(
        title = "Stashes",
        icon = painterResource("stash.svg"),
        items = stashList,
        itemContent = { stashInfo ->
            StashRow(
                stash = stashInfo,
                onClick = { onStashSelected(stashInfo) }
            )
        }
    )

}

@Composable
private fun StashRow(stash: RevCommit, onClick: () -> Unit) {
    SideMenuSubentry(
        text = stash.shortMessage,
        iconResourcePath = "stash.svg",
        onClick = onClick,
    )
}