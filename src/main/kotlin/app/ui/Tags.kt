package app.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.MAX_SIDE_PANEL_ITEMS_HEIGHT
import app.extensions.simpleName
import app.git.GitManager
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SideMenuEntry
import app.ui.components.SideMenuSubentry
import app.ui.components.entryHeight
import app.ui.context_menu.tagContextMenuItems
import org.eclipse.jgit.lib.Ref

@Composable
fun Tags(
    gitManager: GitManager,
    onTagClicked: (Ref) -> Unit,
) {
    val tagsState = gitManager.tags.collectAsState()
    val tags = tagsState.value

    Column {
        SideMenuEntry(
            text = "Tags",
        )

        val tagsHeight = tags.count() * entryHeight
        val maxHeight = if (tagsHeight < MAX_SIDE_PANEL_ITEMS_HEIGHT)
            tagsHeight
        else
            MAX_SIDE_PANEL_ITEMS_HEIGHT

        Box(modifier = Modifier.heightIn(max = maxHeight.dp)) {
            ScrollableLazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items = tags) { tag ->
                    TagRow(
                        tag = tag,
                        onTagClicked = { onTagClicked(tag) },
                        onCheckoutTag = { gitManager.checkoutRef(tag) },
                        onDeleteTag = { gitManager.deleteTag(tag) }
                    )
                }
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagRow(
    tag: Ref,
    onTagClicked: () -> Unit,
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
) {
    ContextMenuArea(
        items = {
            tagContextMenuItems(
                onCheckoutTag = onCheckoutTag,
                onDeleteTag = onDeleteTag,
            )
        }
    ) {
        SideMenuSubentry(
            text = tag.simpleName,
            iconResourcePath = "tag.svg",
            onClick = onTagClicked,
        )
    }
}