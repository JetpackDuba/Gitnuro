package com.jetpackduba.gitnuro.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.PullType
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.ignoreKeyEvents
import com.jetpackduba.gitnuro.keybindings.Keybinding
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.keyBinding
import com.jetpackduba.gitnuro.repositoryopen.RepositoryOpenViewModel
import com.jetpackduba.gitnuro.theme.notoSansMonoFontFamily
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.context_menu.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val DISABLED_BUTTON_ALPHA = 0.5F

@Composable
fun Menu(
    modifier: Modifier,
    viewModel: RepositoryOpenViewModel,
    onCreateBranch: () -> Unit,
    onOpenAnotherRepository: (String) -> Unit,
    onOpenAnotherRepositoryFromPicker: () -> Unit,
    onStashWithMessage: () -> Unit,
    onQuickActions: () -> Unit,
    onShowSettingsDialog: () -> Unit,
    showOpenPopup: Boolean,
    onShowOpenPopupChange: (Boolean) -> Unit,
) {
    val isPullWithRebaseDefault by viewModel.isPullWithRebaseDefault.collectAsState(false)
    val lastLoadedTabs by viewModel.lastLoadedTabs.collectAsState()
    val hasUncommittedChanges by viewModel.hasUncommittedChanges.collectAsState()
    val stashesState by viewModel.stashesState.collectAsState()
    val remotes by viewModel.remoteState.collectAsState()
    val hasRemotes = remotes.remotes.isNotEmpty()
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

        val pullTooltip = if (!hasRemotes) {
            stringResource(Res.string.menu_pull_tooltip_disabled)
        } else if (isPullWithRebaseDefault) {
            stringResource(Res.string.menu_pull_rebase)
        } else {
            stringResource(Res.string.menu_pull_default)
        }

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_pull),
            tooltipText = pullTooltip,
            enabled = hasRemotes,
            icon = painterResource(Res.drawable.download),
            keybinding = KeybindingOption.PULL.keyBinding,
            onClick = { viewModel.pull(PullType.DEFAULT) },
            extendedListItems = pullContextMenuItems(
                isPullWithRebaseDefault = isPullWithRebaseDefault,
                onPullWith = {
                    // Do the reverse of the default
                    val pullType = if (isPullWithRebaseDefault) {
                        PullType.MERGE
                    } else {
                        PullType.REBASE
                    }

                    viewModel.pull(pullType = pullType)
                },
                onFetchAll = {
                    viewModel.fetchAll()
                }
            )
        )

        val pushTooltip = if (!hasRemotes) {
            stringResource(Res.string.menu_push_tooltip_disabled)
        } else {
            stringResource(Res.string.menu_push_tooltip)
        }

        ExtendedMenuButton(
            title = stringResource(Res.string.menu_push),
            tooltipText = pushTooltip,
            enabled = hasRemotes,
            icon = painterResource(Res.drawable.upload),
            onClick = { viewModel.push(force = false, pushTags = false) },
            keybinding = KeybindingOption.PUSH.keyBinding,
            extendedListItems = pushContextMenuItems(
                onPushWithTags = {
                    viewModel.push(force = false, pushTags = true)
                },
                onForcePush = {
                    viewModel.push(force = true, pushTags = false)
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

        val enableStash = hasUncommittedChanges

        val stashTooltipText = if (enableStash) {
            stringResource(Res.string.menu_stash_tooltip)
        } else {
            stringResource(Res.string.menu_stash_tooltip_disabled)
        }

        ExtendedMenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_stash),
            tooltipText = stashTooltipText,
            enabled = enableStash,
            icon = painterResource(Res.drawable.stash),
            keybinding = if (enableStash) KeybindingOption.STASH.keyBinding else null,
            onClick = { viewModel.stash() },
            extendedListItems = stashContextMenuItems(
                onStashWithMessage = onStashWithMessage
            )
        )

        val enablePopStash = stashesState.stashes.isNotEmpty()

        val popStashTooltipText = if (enablePopStash) {
            stringResource(Res.string.menu_pop_stash_tooltip)
        } else {
            stringResource(Res.string.menu_pop_stash_tooltip_disabled)
        }

        MenuButton(
            title = stringResource(Res.string.menu_pop_stash),
            icon = painterResource(Res.drawable.apply_stash),
            keybinding = KeybindingOption.STASH_POP.keyBinding,
            tooltip = popStashTooltipText,
            enabled = enablePopStash,
        ) { viewModel.popStash() }

        Spacer(modifier = Modifier.weight(1f))

        MenuButton(
            modifier = Modifier.padding(end = 4.dp),
            title = stringResource(Res.string.menu_terminal),
            icon = painterResource(Res.drawable.terminal),
            onClick = { viewModel.openTerminal() },
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
                    text = stringResource(Res.string.menu_open_dialog_title),
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
    val keybinding = if (enabled) keybinding else null

    InstantTooltip(
        text = tooltip,
        enabled = tooltipEnabled,
        trailingContent = if (keybinding != null) {
            { KeybindingHint(keybinding) }
        } else {
            null
        }
    ) {
        val color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1F else DISABLED_BUTTON_ALPHA)

        Column(
            modifier = modifier
                .ignoreKeyEvents()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colors.surface)
                .handMouseClickable(enabled) { onClick() }
                .size(56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .size(24.dp),
                tint = color,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                maxLines = 1,
                textAlign = TextAlign.Center,
                color = color,
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
    val color = MaterialTheme.colors.onBackground.copy(alpha = if (enabled) 1F else DISABLED_BUTTON_ALPHA)
    val keybinding = if (enabled) keybinding else null

    Row(
        modifier = modifier
            .size(width = 64.dp, height = 56.dp)
            .ignoreKeyEvents()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.surface)
            .handMouseClickable(enabled) { onClick() }
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
                    tint = color,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.caption,
                    color = color,
                    maxLines = 1,
                )
            }
        }

        DropDownMenu(
            enabled = enabled,
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
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )

            }
        }
    }
}
