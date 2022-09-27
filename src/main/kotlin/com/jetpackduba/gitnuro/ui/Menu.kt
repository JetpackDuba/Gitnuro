@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.theme.primaryTextColor
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.viewmodels.MenuViewModel

// TODO Add tooltips to all the buttons
@Composable
fun Menu(
    modifier: Modifier,
    menuViewModel: MenuViewModel,
    onRepositoryOpen: () -> Unit,
    onCreateBranch: () -> Unit,
    onStashWithMessage: () -> Unit,
) {
    var showAdditionalOptionsDropDownMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuButton(
            modifier = Modifier.padding(start = 8.dp),
            title = "Open",
            icon = painterResource("open.svg"),
            onClick = {
                onRepositoryOpen()
            }
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

        Box {
            IconMenuButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp),
                icon = painterResource("more_vert.svg"),
                onClick = {
                    showAdditionalOptionsDropDownMenu = true
                },
            )
            DropdownMenu(
                expanded = showAdditionalOptionsDropDownMenu,
                content = {
                    val menuOptions = remember {
                        repositoryAdditionalOptionsMenu(
                            onOpenRepositoryOnFileExplorer = { menuViewModel.openFolderInFileExplorer() },
                            onForceRepositoryRefresh = { menuViewModel.refresh() },
                        )
                    }
                    for (item in menuOptions) {
                        DropDownContent(
                            dropDownContentData = item,
                            onDismiss = { showAdditionalOptionsDropDownMenu = false }
                        )
                    }
                },
                onDismissRequest = { showAdditionalOptionsDropDownMenu = false }
            )
        }
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val iconColor = if (enabled) {
        MaterialTheme.colors.primaryVariant
    } else {
        MaterialTheme.colors.secondaryVariant //todo this color isn't specified anywhere
    }

    Box(
        modifier = modifier
            .handMouseClickable { if (enabled) onClick() }
            .border(ButtonDefaults.outlinedBorder, RoundedCornerShape(4.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp),
                colorFilter = ColorFilter.tint(iconColor),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
            )
        }
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
    val iconColor = if (enabled) {
        MaterialTheme.colors.primaryVariant
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    var showDropDownMenu by remember { mutableStateOf(false) }

    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Row(
            modifier = Modifier
                .handMouseClickable { if (enabled) onClick() }
                .border(ButtonDefaults.outlinedBorder, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp),
                colorFilter = ColorFilter.tint(iconColor),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
            )
        }

        Box(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .border(ButtonDefaults.outlinedBorder, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .handMouseClickable {
                    showDropDownMenu = true
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colors.primaryTextColor,
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
            .pointerHoverIcon(PointerIconDefaults.Hand),
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

