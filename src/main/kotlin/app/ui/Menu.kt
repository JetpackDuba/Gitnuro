@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.primaryTextColor
import app.ui.context_menu.DropDownContent
import app.ui.context_menu.DropDownContentData
import app.ui.context_menu.pullContextMenuItems
import app.ui.context_menu.pushContextMenuItems
import app.viewmodels.MenuViewModel

// TODO Add tooltips to all the buttons
@Composable
fun Menu(
    menuViewModel: MenuViewModel,
    onRepositoryOpen: () -> Unit,
    onCreateBranch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
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
                onForcePush = {
                    menuViewModel.push(true)
                }
            )
        )

        Spacer(modifier = Modifier.width(16.dp))

        MenuButton(
            title = "Branch",
            icon = painterResource("branch.svg"),
            onClick = {
                onCreateBranch()
            },
        )

        Spacer(modifier = Modifier.width(16.dp))

        MenuButton(
            title = "Stash",
            icon = painterResource("stash.svg"),
            onClick = { menuViewModel.stash() },
        )
        MenuButton(
            title = "Pop",
            icon = painterResource("apply_stash.svg"),
            onClick = { menuViewModel.popStash() },
        )

        Spacer(modifier = Modifier.weight(1f))

        IconMenuButton(
            modifier = Modifier.padding(end = 8.dp),
            icon = painterResource("source.svg"),
            onClick = { menuViewModel.openFolderInFileExplorer() },
        )
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
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clickable { if (enabled) onClick() }
            .border(ButtonDefaults.outlinedBorder, RoundedCornerShape(3.dp))
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
                fontSize = 12.sp,
                color = MaterialTheme.colors.primaryTextColor
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
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    var showDropDownMenu by remember { mutableStateOf(false) }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = modifier
                .clickable { if (enabled) onClick() }
                .border(ButtonDefaults.outlinedBorder, RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp))
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
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.primaryTextColor
                )
            }
        }

        Box(
            modifier = modifier
                .padding(end = 8.dp)
                .width(20.dp)
                .fillMaxHeight()
                .border(ButtonDefaults.outlinedBorder, RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .clickable {
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
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    OutlinedButton(
        modifier = modifier
            .padding(horizontal = 2.dp),
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

