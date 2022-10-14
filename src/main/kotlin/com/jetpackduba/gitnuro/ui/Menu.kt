@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.ignoreKeyEvents
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.viewmodels.MenuViewModel

// TODO Add tooltips to all the buttons
@Composable
fun Menu(
    modifier: Modifier,
    menuViewModel: MenuViewModel,
    onCreateBranch: () -> Unit,
    onGoToWorkspace: () -> Unit,
    onStashWithMessage: () -> Unit,
    onQuickActions: () -> Unit,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuButton(
            modifier = Modifier
                .padding(start = 16.dp),
            title = "Workspace",
            icon = painterResource("computer.svg"),
            onClick = onGoToWorkspace,
            fixedWidth = false,
        )

        Spacer(modifier = Modifier.weight(1f))

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 8.dp),
            title = "Pull",
            icon = painterResource("download.svg"),
            onClick = { menuViewModel.pull() },
            extendedListItems = pullContextMenuItems(
                onPullRebase = {
                    menuViewModel.pull(true)
                },
                onFetchAll = {
                    menuViewModel.fetchAll()
                }
            )
        )

        ExtendedMenuButton(
            title = "Push",
            icon = painterResource("upload.svg"),
            onClick = { menuViewModel.push() },
            extendedListItems = pushContextMenuItems(
                onPushWithTags = {
                    menuViewModel.push(force = false, pushTags = true)
                },
                onForcePush = {
                    menuViewModel.push(force = true)
                }
            )
        )

        Spacer(modifier = Modifier.width(24.dp))

        MenuButton(
            title = "Branch",
            icon = painterResource("branch.svg"),
            onClick = {
                onCreateBranch()
            },
        )

        Spacer(modifier = Modifier.width(24.dp))

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 8.dp),
            title = "Stash",
            icon = painterResource("stash.svg"),
            onClick = { menuViewModel.stash() },
            extendedListItems = stashContextMenuItems(
                onStashWithMessage = onStashWithMessage
            )
        )

        MenuButton(
            title = "Pop",
            icon = painterResource("apply_stash.svg"),
            onClick = { menuViewModel.popStash() },
        )

        Spacer(modifier = Modifier.weight(1f))

        MenuButton(
            title = "Quick actions",
            modifier = Modifier.padding(end = 16.dp),
            icon = painterResource("bolt.svg"),
            fixedWidth = false,
            onClick = onQuickActions,
        )
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    icon: Painter,
    fixedWidth: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .ignoreKeyEvents()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.primary)
            .handMouseClickable { if (enabled) onClick() }
            .run {
                return@run if (fixedWidth) {
                    this.width(100.dp)
                } else
                    this.padding(horizontal = 16.dp)
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            modifier = Modifier
                .padding(vertical = 2.dp)
                .size(24.dp),
            tint = MaterialTheme.colors.onPrimary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 8.dp),
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onPrimary,
        )
    }
}

@Composable
fun ExtendedMenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    icon: Painter,
    onClick: () -> Unit,
    extendedListItems: List<DropDownContentData>,
) {
    var showDropDownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .ignoreKeyEvents()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.primary)
    ) {
        Row(
            modifier = Modifier
                .width(92.dp)
                .handMouseClickable { if (enabled) onClick() },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(24.dp),
                tint = MaterialTheme.colors.onPrimary,
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
            )
        }

        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight()
                .ignoreKeyEvents()
                .handMouseClickable {
                    showDropDownMenu = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(MaterialTheme.colors.onPrimary.copy(alpha = 0.5f))
                    .width(1.dp)
                    .align(Alignment.CenterStart)
            )

            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colors.onPrimary,
            )

            DropdownMenu(
                onDismissRequest = {
                    showDropDownMenu = false
                },
                content = {
                    for (item in extendedListItems) {
                        DropDownContent(item, onDismiss = { showDropDownMenu = false })
                    }
                },
                expanded = showDropDownMenu,
            )
        }
    }
}

@Composable
fun IconMenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter,
    onClick: () -> Unit
) {
    val iconColor = if (enabled) {
        MaterialTheme.colors.primaryVariant
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    IconButton(
        modifier = modifier
            .handOnHover(),
        enabled = enabled,
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp),
                colorFilter = ColorFilter.tint(iconColor),
            )
        }

    }
}

