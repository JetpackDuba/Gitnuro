@file:OptIn(ExperimentalFoundationApi::class)

package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import app.ui.components.SideMenuPanel
import app.ui.components.SideMenuSubentry
import app.ui.context_menu.stashesContextMenuItems
import app.viewmodels.StashStatus
import app.viewmodels.StashesViewModel
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun Stashes(
    stashesViewModel: StashesViewModel,
) {
    val stashStatusState = stashesViewModel.stashStatus.collectAsState()
    val stashStatus = stashStatusState.value
    val isExpanded by stashesViewModel.isExpanded.collectAsState()

    val stashList = if (stashStatus is StashStatus.Loaded)
        stashStatus.stashes
    else
        listOf()

    SideMenuPanel(
        title = "Stashes",
        icon = painterResource("stash.svg"),
        items = stashList,
        isExpanded = isExpanded,
        onExpand = { stashesViewModel.onExpand() },
        itemContent = { stash ->
            StashRow(
                stash = stash,
                onClick = { stashesViewModel.selectTab(stash) },
                contextItems = stashesContextMenuItems(
                    onApply = { stashesViewModel.applyStash(stash) },
                    onPop = {
                        stashesViewModel.popStash(stash)
                    },
                    onDelete = {
                        stashesViewModel.deleteStash(stash)
                    },
                )
            )
        }
    )

}

@Composable
private fun StashRow(
    stash: RevCommit,
    onClick: () -> Unit,
    contextItems: List<ContextMenuItem>,
) {
    ContextMenuArea(
        items = { contextItems }
    ) {
        SideMenuSubentry(
            text = stash.shortMessage,
            iconResourcePath = "stash.svg",
            onClick = onClick,
        )
    }
}