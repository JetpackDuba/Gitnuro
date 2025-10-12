@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppConstants
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.AppTheme
import com.jetpackduba.gitnuro.theme.backgroundSelected
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.BottomInfoBar
import com.jetpackduba.gitnuro.ui.components.tooltip.DelayedTooltip
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.dialogs.AppInfoDialog
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource


@Composable
fun WelcomePage(
    tabViewModel: TabViewModel,
    onShowCloneDialog: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val recentlyOpenedRepositories by tabViewModel.appStateManager.latestOpenedRepositoriesPaths.collectAsState()
    val newUpdate by tabViewModel.update.collectAsState()

    WelcomeView(
        recentlyOpenedRepositories,
        newUpdate,
        onShowCloneDialog = onShowCloneDialog,
        onShowSettings = onShowSettings,
        onOpenRepository = {
            val repo = tabViewModel.openDirectoryPicker()

            if (repo != null) {
                tabViewModel.openRepository(repo)
            }
        },
        onStartRepository = {
            val dir = tabViewModel.openDirectoryPicker()

            if (dir != null) {
                tabViewModel.initLocalRepository(dir)
            }
        },
        onOpenKnownRepository = { tabViewModel.openRepository(it) },
        onOpenUrlInBrowser = { tabViewModel.openUrlInBrowser(it) },
        onRemoveRepositoryFromRecent = { tabViewModel.removeRepositoryFromRecent(it) }

    )
}

@Preview
@Composable
fun WelcomeViewPreview() {
    AppTheme(
        customTheme = null
    ) {
        val recentRepositories = (0..10).map {
            "/home/user/sample$it"
        }
        WelcomeView(
            recentlyOpenedRepositories = recentRepositories,
            newUpdate = null,
            onShowCloneDialog = {},
            onShowSettings = {},
            onOpenRepository = {},
            onOpenKnownRepository = {},
            onStartRepository = {},
            onOpenUrlInBrowser = {},
            onRemoveRepositoryFromRecent = {},
        )
    }
}

@Composable
fun WelcomeView(
    recentlyOpenedRepositories: List<String>,
    newUpdate: Update?,
    onShowCloneDialog: () -> Unit,
    onShowSettings: () -> Unit,
    onOpenRepository: () -> Unit,
    onOpenKnownRepository: (String) -> Unit,
    onStartRepository: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
    onRemoveRepositoryFromRecent: (String) -> Unit,
) {

    var showAdditionalInfo by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    val welcomeViewFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusable(true)
            .focusRequester(welcomeViewFocusRequester)
            .background(MaterialTheme.colors.surface)
            .onPreviewKeyEvent {
                when {
                    it.matchesBinding(KeybindingOption.OPEN_REPOSITORY) -> {
                        onOpenRepository()
                        true
                    }

                    it.matchesBinding(KeybindingOption.SETTINGS) -> {
                        onShowSettings()
                        true
                    }

                    else -> false
                }
            },
    ) {

        Column(
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp, BiasAlignment.Vertical(-0.5f)),
        ) {

            Text(
                text = AppConstants.APP_NAME,
                style = MaterialTheme.typography.h1,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                HomeButtons(
                    onOpenRepository = onOpenRepository,
                    onStartRepository = onStartRepository,
                    onShowCloneView = onShowCloneDialog,
                    onShowAdditionalInfo = { showAdditionalInfo = true },
                    onShowSettings = onShowSettings,
                    onOpenUrlInBrowser = onOpenUrlInBrowser,
                )

                RecentRepositories(
                    recentlyOpenedRepositories,
                    canRepositoriesBeRemoved = true,
                    onOpenKnownRepository = onOpenKnownRepository,
                    onRemoveRepositoryFromRecent = onRemoveRepositoryFromRecent,
                    searchFieldFocusRequester = searchFocusRequester,
                )
            }
        }

        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.2f))
        )

        BottomInfoBar(
            newUpdate = newUpdate,
            onOpenUrlInBrowser = onOpenUrlInBrowser,
        )
    }

    LaunchedEffect(recentlyOpenedRepositories.isEmpty()) {
        if (recentlyOpenedRepositories.isEmpty()) {
            welcomeViewFocusRequester.requestFocus()
        } else {
            searchFocusRequester.requestFocus()
        }
    }

    if (showAdditionalInfo) {
        AppInfoDialog(
            onClose = { showAdditionalInfo = false },
            onOpenUrlInBrowser = onOpenUrlInBrowser
        )
    }
}

@Composable
fun HomeButtons(
    onOpenRepository: () -> Unit,
    onStartRepository: () -> Unit,
    onShowCloneView: () -> Unit,
    onShowAdditionalInfo: () -> Unit,
    onShowSettings: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
) {
    Column {
        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = stringResource(Res.string.home_button_open_repository),
            painter = painterResource(Res.drawable.open),
            onClick = onOpenRepository
        )

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = stringResource(Res.string.home_button_clone_repository),
            painter = painterResource(Res.drawable.download),
            onClick = onShowCloneView
        )

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = stringResource(Res.string.home_button_start_repository),
            painter = painterResource(Res.drawable.open),
            onClick = onStartRepository
        )

        Text(
            stringResource(Res.string.home_button_additional_information),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        IconTextButton(
            title = stringResource(Res.string.home_button_source_code),
            painter = painterResource(Res.drawable.code),
            onClick = {
                onOpenUrlInBrowser("https://github.com/JetpackDuba/Gitnuro")
            }
        )

        IconTextButton(
            title = stringResource(Res.string.home_button_report_bug),
            painter = painterResource(Res.drawable.bug),
            onClick = {
                onOpenUrlInBrowser("https://github.com/JetpackDuba/Gitnuro/issues")
            }
        )

        IconTextButton(
            title = stringResource(Res.string.home_button_additional_information),
            painter = painterResource(Res.drawable.info),
            onClick = onShowAdditionalInfo
        )

        IconTextButton(
            title = stringResource(Res.string.home_button_settings),
            painter = painterResource(Res.drawable.settings),
            onClick = onShowSettings
        )
    }
}

@Composable
fun RecentRepositories(
    recentlyOpenedRepositories: List<String>,
    canRepositoriesBeRemoved: Boolean,
    onRemoveRepositoryFromRecent: (String) -> Unit,
    onOpenKnownRepository: (String) -> Unit,
    searchFieldFocusRequester: FocusRequester,
) {
    Column(
        modifier = Modifier
            .padding(start = 32.dp)
            .width(600.dp)
            .height(400.dp),
    ) {
        if (recentlyOpenedRepositories.isEmpty()) {
            Text(
                stringResource(Res.string.home_recent_repositories_list_empty),
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            RecentRepositoriesList(
                recentlyOpenedRepositories = recentlyOpenedRepositories,
                canRepositoriesBeRemoved = canRepositoriesBeRemoved,
                onRemoveRepositoryFromRecent = onRemoveRepositoryFromRecent,
                onOpenKnownRepository = onOpenKnownRepository,
                searchFieldFocusRequester = searchFieldFocusRequester,
            )
        }
    }
}

@Composable
fun RecentRepositoriesList(
    recentlyOpenedRepositories: List<String>,
    canRepositoriesBeRemoved: Boolean,
    searchFieldFocusRequester: FocusRequester,
    onRemoveRepositoryFromRecent: (String) -> Unit,
    onOpenKnownRepository: (String) -> Unit,
    onExitClicked: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var filter by remember {
        mutableStateOf("")
    }

    var focusedItemIndex by remember { mutableStateOf(0) }

    var isSearchFocused by remember { mutableStateOf(false) }

    val filteredRepositories = remember(filter, recentlyOpenedRepositories) {
        if (filter.isBlank()) {
            recentlyOpenedRepositories
        } else {
            recentlyOpenedRepositories.filter { repository ->
                repository.lowercaseContains(filter)
            }
        }
    }

    Column(
        modifier = Modifier.onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) {
                return@onPreviewKeyEvent false
            }

            when {
                it.matchesBinding(KeybindingOption.DOWN) -> {
                    if (focusedItemIndex < filteredRepositories.lastIndex) {
                        focusedItemIndex += 1
                        scope.launch { listState.animateScrollToItem(focusedItemIndex) }
                    }
                    true
                }

                it.matchesBinding(KeybindingOption.UP) -> {
                    if (focusedItemIndex > 0) {
                        focusedItemIndex -= 1
                        scope.launch { listState.animateScrollToItem(focusedItemIndex) }
                    }
                    true
                }

                it.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) -> {
                    val repo = filteredRepositories.getOrNull(focusedItemIndex)
                    if (repo != null && isSearchFocused) {
                        onOpenKnownRepository(repo)
                    }
                    true
                }

                it.matchesBinding(KeybindingOption.EXIT) -> {
                    if (filter.isEmpty()) {
                        onExitClicked()
                        true
                    } else {
                        false
                    }
                }

                else -> {
                    false
                }
            }
        }
    ) {
        AdjustableOutlinedTextField(
            modifier = Modifier
                .focusRequester(searchFieldFocusRequester)
                .onFocusChanged { isSearchFocused = it.isFocused }
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.EXIT) && keyEvent.type == KeyEventType.KeyDown) {
                        filter = ""
                        true
                    } else
                        false
                },
            value = filter,
            onValueChange = { filter = it },
            singleLine = true,
            hint = stringResource(Res.string.home_recent_repositories_search_label),
            trailingIcon = {
                if (filter.isNotEmpty()) {
                    IconButton(
                        onClick = { filter = "" },
                        modifier = Modifier
                            .size(16.dp)
                            .handOnHover(),
                    ) {
                        Icon(
                            painterResource(Res.drawable.close),
                            contentDescription = null,
                            tint = if (filter.isEmpty()) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground
                        )
                    }
                }
            }
        )

        LaunchedEffect(filteredRepositories) {
            if (filter.isNotEmpty() && filteredRepositories.isNotEmpty()) {
                focusedItemIndex = 0
            }
        }

        Box(modifier = Modifier.padding(top = 4.dp)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(items = filteredRepositories) { index, repo ->
                    val repoDirName = repo.dirName
                    val repoDirPath = repo.dirPath
                    val hoverInteraction = remember { MutableInteractionSource() }
                    val isHovered by hoverInteraction.collectIsHoveredAsState()

                    LaunchedEffect(isHovered) {
                        if (isHovered) {
                            focusedItemIndex = index
                        }
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .fillMaxWidth()
                            .hoverable(hoverInteraction)
                            .handMouseClickable { onOpenKnownRepository(repo) }
                            .backgroundIf(
                                isSearchFocused && index == focusedItemIndex,
                                MaterialTheme.colors.backgroundSelected
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = repoDirName,
                                style = MaterialTheme.typography.body2,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primaryVariant,
                                overflow = TextOverflow.Ellipsis,
                            )

                            InstantTooltip(repoDirPath) {
                                Text(
                                    text = if (repoDirPath.startsWith(System.getProperty("user.home"))) {
                                        "~${repoDirPath.removePrefix(System.getProperty("user.home"))}"
                                    } else {
                                        repoDirPath
                                    },
                                    style = MaterialTheme.typography.body2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier,
                                    maxLines = 1,
                                    color = MaterialTheme.colors.onBackgroundSecondary
                                )
                            }
                        }


                        val buttonAlpha = if (canRepositoriesBeRemoved && isHovered) {
                            1f
                        } else {
                            0f
                        }

                        DelayedTooltip(
                            text = stringResource(Res.string.home_recent_repositories_remove_repository),
                        ) {
                            IconButton(
                                onClick = { onRemoveRepositoryFromRecent(repo) },
                                enabled = canRepositoriesBeRemoved && isHovered,
                                modifier = Modifier.alpha(buttonAlpha)
                                    .size(24.dp)
                                    .handOnHover(),
                            ) {
                                Icon(
                                    painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.home_recent_repositories_remove_repository),
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colors.onBackgroundSecondary
                                )
                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
fun ButtonTile(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .size(width = 280.dp, height = 56.dp)
            .handOnHover(),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = MaterialTheme.colors.primary)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(24.dp),
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
            )

            Text(
                text = title,
                maxLines = 1,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onPrimary,
            )
        }
    }
}

@Composable
fun IconTextButton(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter,
    iconColor: Color = MaterialTheme.colors.primaryVariant,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .size(width = 280.dp, height = 40.dp)
            .handOnHover(),
        colors = textButtonColors(),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(24.dp),
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconColor),
            )

            Text(
                text = title,
                maxLines = 1,
            )
        }
    }
}

