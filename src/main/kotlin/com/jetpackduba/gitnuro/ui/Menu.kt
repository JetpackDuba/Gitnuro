@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.ignoreKeyEvents
import com.jetpackduba.gitnuro.git.remote_operations.PullType
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.viewmodels.MenuViewModel

// TODO Add tooltips to all the buttons
@Composable
fun Menu(
    modifier: Modifier,
    menuViewModel: MenuViewModel = gitnuroViewModel(),
    onCreateBranch: () -> Unit,
    onOpenAnotherRepository: () -> Unit,
    onStashWithMessage: () -> Unit,
    onQuickActions: () -> Unit,
    onShowSettingsDialog: () -> Unit,
) {
    val isPullWithRebaseDefault by menuViewModel.isPullWithRebaseDefault.collectAsState()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuButton(
            modifier = Modifier
                .padding(start = 16.dp),
            title = "Open",
            icon = painterResource(AppIcons.OPEN),
            onClick = onOpenAnotherRepository,
        )

        Spacer(modifier = Modifier.weight(1f))

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = "Pull",
            icon = painterResource(AppIcons.DOWNLOAD),
            onClick = { menuViewModel.pull(PullType.DEFAULT) },
            extendedListItems = pullContextMenuItems(
                isPullWithRebaseDefault = isPullWithRebaseDefault,
                onPullWith = {
                    // Do the reverse of the default
                    val pullType = if (isPullWithRebaseDefault) {
                        PullType.MERGE
                    } else {
                        PullType.REBASE
                    }

                    menuViewModel.pull(pullType = pullType)
                },
                onFetchAll = {
                    menuViewModel.fetchAll()
                }
            )
        )

        ExtendedMenuButton(
            title = "Push",
            icon = painterResource(AppIcons.UPLOAD),
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

        Spacer(modifier = Modifier.width(32.dp))

        MenuButton(
            title = "Branch",
            icon = painterResource(AppIcons.BRANCH),
        ) {
            onCreateBranch()
        }

//        MenuButton(
//            title = "Merge",
//            icon = painterResource("merge.svg"),
//        ) {
//            onCreateBranch()
//        }

        Spacer(modifier = Modifier.width(32.dp))

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = "Stash",
            icon = painterResource(AppIcons.STASH),
            onClick = { menuViewModel.stash() },
            extendedListItems = stashContextMenuItems(
                onStashWithMessage = onStashWithMessage
            )
        )

        MenuButton(
            title = "Pop",
            icon = painterResource(AppIcons.APPLY_STASH),
        ) { menuViewModel.popStash() }

        Spacer(modifier = Modifier.weight(1f))

        MenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = "Terminal",
            icon = painterResource(AppIcons.TERMINAL),
            onClick = { menuViewModel.openTerminal() },
        )

        MenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = "Actions",
            icon = painterResource(AppIcons.BOLT),
            onClick = onQuickActions,
        )

        MenuButton(
            modifier = Modifier.padding(end = 16.dp),
            title = "Settings",
            icon = painterResource(AppIcons.SETTINGS),
            onClick = onShowSettingsDialog,
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
    Column(
        modifier = modifier
            .ignoreKeyEvents()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface)
            .handMouseClickable { if (enabled) onClick() }
            .size(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = title,
            modifier = Modifier
                .size(24.dp),
            tint = MaterialTheme.colors.onBackground,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.caption,
            maxLines = 1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onBackground,
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
    extendedListItems: List<ContextMenuElement>,
) {
    Row(
        modifier = modifier
            .size(width = 64.dp, height = 56.dp)
            .ignoreKeyEvents()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface)
            .handMouseClickable { if (enabled) onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(24.dp),
                tint = MaterialTheme.colors.onBackground,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
            )
        }

        DropdownMenu(
            items = { extendedListItems }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .ignoreKeyEvents(),
                contentAlignment = Alignment.Center,
            ) {

                Icon(
                    painterResource(AppIcons.EXPAND_MORE),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )

            }
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

