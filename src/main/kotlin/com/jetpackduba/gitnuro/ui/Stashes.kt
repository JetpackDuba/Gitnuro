package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.ui.components.SideMenuPanel
import com.jetpackduba.gitnuro.ui.components.SideMenuSubentry
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.stashesContextMenuItems
import com.jetpackduba.gitnuro.viewmodels.StashStatus
import com.jetpackduba.gitnuro.viewmodels.StashesViewModel
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
    contextItems: List<ContextMenuElement>,
) {
    ContextMenu(
        items = { contextItems }
    ) {
        SideMenuSubentry(
            text = stash.shortMessage,
            iconResourcePath = "stash.svg",
            onClick = onClick,
        )
    }
}