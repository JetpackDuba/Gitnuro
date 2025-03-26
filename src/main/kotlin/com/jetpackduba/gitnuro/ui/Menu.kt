@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.ignoreKeyEvents
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.git.remote_operations.PullType
import com.jetpackduba.gitnuro.keybindings.Keybinding
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.keyBinding
import com.jetpackduba.gitnuro.theme.notoSansMonoFontFamily
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.viewmodels.MenuViewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// TODO Add tooltips to all the buttons
@Composable
fun Menu(
    modifier: Modifier,
    menuViewModel: MenuViewModel,
    onCreateBranch: () -> Unit,
    onOpenAnotherRepository: (String) -> Unit,
    onOpenAnotherRepositoryFromPicker: () -> Unit,
    onStashWithMessage: () -> Unit,
    onQuickActions: () -> Unit,
    onShowSettingsDialog: () -> Unit,
    showOpenPopup: Boolean,
    onShowOpenPopupChange: (Boolean) -> Unit,
) {
    val isPullWithRebaseDefault by menuViewModel.isPullWithRebaseDefault.collectAsState()
    val lastLoadedTabs by menuViewModel.lastLoadedTabs.collectAsState()
    val (position, setPosition) = remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MenuButton(
            modifier = Modifier
                .padding(start = 16.dp)
                .onGloballyPositioned { setPosition(it) },
            title = stringResource(Res.string.menu_open),
            icon = painterResource(Res.drawable.open),
            keybinding = KeybindingOption.OPEN_REPOSITORY.keyBinding,
            tooltip = stringResource(Res.string.menu_open_tooltip),
            tooltipEnabled = !showOpenPopup,
            onClick = { onShowOpenPopupChange(true) },
        )

        Spacer(modifier = Modifier.weight(1f))

        val pullTooltip = if (isPullWithRebaseDefault) {
            stringResource(Res.string.menu_pull_rebase)
        } else {
            stringResource(Res.string.menu_pull_default)
        }


        ExtendedMenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_pull),
            tooltipText = pullTooltip,
            icon = painterResource(Res.drawable.download),
            keybinding = KeybindingOption.PULL.keyBinding,
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
            title = stringResource(Res.string.menu_push),
            tooltipText = stringResource(Res.string.menu_push_tooltip),
            icon = painterResource(Res.drawable.upload),
            onClick = { menuViewModel.push() },
            keybinding = KeybindingOption.PUSH.keyBinding,
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
            title = stringResource(Res.string.menu_branch),
            icon = painterResource(Res.drawable.branch),
            onClick = {
                onCreateBranch()
            },
            tooltip = stringResource(Res.string.menu_branch_tooltip),
            keybinding = KeybindingOption.BRANCH_CREATE.keyBinding,
        )


        Spacer(modifier = Modifier.width(32.dp))

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_stash),
            tooltipText = stringResource(Res.string.menu_stash_tooltip),
            icon = painterResource(Res.drawable.stash),
            keybinding = KeybindingOption.STASH.keyBinding,
            onClick = { menuViewModel.stash() },
            extendedListItems = stashContextMenuItems(
                onStashWithMessage = onStashWithMessage
            )
        )

        MenuButton(
            title = stringResource(Res.string.menu_pop_stash),
            icon = painterResource(Res.drawable.apply_stash),
            keybinding = KeybindingOption.STASH_POP.keyBinding,
            tooltip = stringResource(Res.string.menu_pop_stash_tooltip),
        ) { menuViewModel.popStash() }

        Spacer(modifier = Modifier.weight(1f))

        MenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_terminal),
            icon = painterResource(Res.drawable.terminal),
            onClick = { menuViewModel.openTerminal() },
            tooltip = stringResource(Res.string.menu_terminal_tooltip),
            keybinding = null,
        )

        MenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_actions),
            icon = painterResource(Res.drawable.bolt),
            onClick = onQuickActions,
            tooltip = stringResource(Res.string.menu_actions_tooltip),
            keybinding = null,
        )

        Box(
            modifier = Modifier.padding(end = 16.dp)
        ) {
            MenuButton(
                title = stringResource(Res.string.menu_settings),
                icon = painterResource(Res.drawable.settings),
                onClick = onShowSettingsDialog,
                tooltip = stringResource(Res.string.menu_settings_tooltip),
                keybinding = KeybindingOption.SETTINGS.keyBinding,
            )
        }
    }

    if (showOpenPopup && position != null) {
        val boundsInRoot = position.boundsInRoot()

        Popup(
            popupPositionProvider =
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize,
                ): IntOffset {
                    return IntOffset(boundsInRoot.left.toInt(), boundsInRoot.bottom.toInt())
                }
            },
            onDismissRequest = { onShowOpenPopupChange(false) },
            properties = PopupProperties(focusable = true),
        ) {
            val searchFocusRequester = remember { FocusRequester() }

            Column(
                modifier = Modifier
                    .width(600.dp)
                    .heightIn(max = 600.dp)
                    .background(MaterialTheme.colors.surface)
                    .border(2.dp, MaterialTheme.colors.onBackground.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                PrimaryButton(
                    text = "Open a repository",
                    onClick = {
                        onShowOpenPopupChange(false)
                        onOpenAnotherRepositoryFromPicker()
                    },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                )

                Box(modifier = Modifier.weight(1f)) {
                    RecentRepositoriesList(
                        recentlyOpenedRepositories = lastLoadedTabs,
                        canRepositoriesBeRemoved = false,
                        searchFieldFocusRequester = searchFocusRequester,
                        onRemoveRepositoryFromRecent = {},
                        onOpenKnownRepository = {
                            onShowOpenPopupChange(false)
                            onOpenAnotherRepository(it)
                        },
                        onExitClicked = {
                            onShowOpenPopupChange(false)
                        }
                    )
                }
            }

            LaunchedEffect(Unit) {
                searchFocusRequester.requestFocus()
            }
        }
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    icon: Painter,
    keybinding: Keybinding?,
    tooltip: String,
    tooltipEnabled: Boolean = true,
    onClick: () -> Unit,
) {
    InstantTooltip(
        text = tooltip,
        enabled = tooltipEnabled,
        trailingContent = if (keybinding != null) {
            { KeybindingHint(keybinding) }
        } else {
            null
        }
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
}

@Composable
fun KeybindingHint(keybinding: Keybinding) {
    val parts = remember(keybinding) { getParts(keybinding) }.joinToString("+")

    Text(
        parts,
        fontFamily = notoSansMonoFontFamily,
        fontSize = MaterialTheme.typography.caption.fontSize,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.onBackgroundSecondary,
    )
}

@Preview
@Composable
fun KeybindingHintPartPreview() {
    KeybindingHintPart("CTRL")
}

@Composable
fun KeybindingHintPart(part: String) {
    Text(
        text = part,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colors.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(2.dp, MaterialTheme.colors.primary, RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.primary.copy(alpha = 0.05f))
            .padding(horizontal = 4.dp, vertical = 4.dp)

    )
}

fun getParts(keybinding: Keybinding): List<String> {
    val parts = mutableListOf<String>()

    if (keybinding.control) {
        parts.add("Ctrl")
    }

    if (keybinding.meta) {
        parts.add("⌘")
    }

    if (keybinding.alt) {
        parts.add("Alt")
    }

    if (keybinding.shift) {
        parts.add("Shift")
    }

    val key = when (keybinding.key) {
        Key.A -> "A"
        Key.B -> "B"
        Key.C -> "C"
        Key.D -> "D"
        Key.E -> "E"
        Key.F -> "F"
        Key.G -> "G"
        Key.H -> "H"
        Key.I -> "I"
        Key.J -> "J"
        Key.K -> "K"
        Key.L -> "L"
        Key.M -> "M"
        Key.N -> "N"
        Key.O -> "O"
        Key.P -> "P"
        Key.Q -> "Q"
        Key.R -> "R"
        Key.S -> "S"
        Key.T -> "T"
        Key.U -> "U"
        Key.V -> "V"
        Key.W -> "W"
        Key.X -> "X"
        Key.Y -> "Y"
        Key.Z -> "Z"
        Key.Tab -> "Tab"
        else -> throw NotImplementedError("Key not implemented")
    }

    parts.add(key)

    return parts
}

@Composable
fun ExtendedMenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    title: String,
    tooltipText: String,
    icon: Painter,
    keybinding: Keybinding?,
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
        InstantTooltip(
            text = tooltipText,
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            trailingContent = if (keybinding != null) {
                { KeybindingHint(keybinding) }
            } else {
                null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
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
        }

        DropDownMenu(
            items = { extendedListItems }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .ignoreKeyEvents(),
                contentAlignment = Alignment.Center,
            ) {

                Icon(
                    painterResource(Res.drawable.expand_more),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(16.dp)
                )

            }
        }
    }
}
