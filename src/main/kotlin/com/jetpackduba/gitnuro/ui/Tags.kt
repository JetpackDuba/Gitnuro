package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.ui.components.SideMenuPanel
import com.jetpackduba.gitnuro.ui.components.SideMenuSubentry
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.tagContextMenuItems
import com.jetpackduba.gitnuro.viewmodels.TagsViewModel
import org.eclipse.jgit.lib.Ref

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Tags(
    tagsViewModel: TagsViewModel = gitnuroViewModel(),
) {
    val tagsState = tagsViewModel.tags.collectAsState()
    val tags = tagsState.value
    val isExpanded by tagsViewModel.isExpanded.collectAsState()

    SideMenuPanel(
        title = "Tags",
        items = tags,
        icon = painterResource("tag.svg"),
        isExpanded = isExpanded,
        onExpand = { tagsViewModel.onExpand() },
        itemContent = { tag ->
            TagRow(
                tag = tag,
                onTagClicked = { tagsViewModel.selectTag(tag) },
                onCheckoutTag = { tagsViewModel.checkoutRef(tag) },
                onDeleteTag = { tagsViewModel.deleteTag(tag) }
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagRow(
    tag: Ref,
    onTagClicked: () -> Unit,
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
) {
    ContextMenu(
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