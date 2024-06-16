@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppConstants
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.theme.AppTheme
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.ui.dialogs.AppInfoDialog
import com.jetpackduba.gitnuro.updates.Update
import com.jetpackduba.gitnuro.viewmodels.TabViewModel


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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
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

    if (showAdditionalInfo) {
        AppInfoDialog(
            onClose = { showAdditionalInfo = false },
            onOpenUrlInBrowser = onOpenUrlInBrowser
        )
    }
}

@Composable
private fun BottomInfoBar(
    newUpdate: Update?,
    onOpenUrlInBrowser: (String) -> Unit,
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.weight(1f, true))

        if (newUpdate != null) {
            SecondaryButton(
                text = "Update ${newUpdate.appVersion} available",
                onClick = { onOpenUrlInBrowser(newUpdate.downloadUrl) },
                backgroundButton = MaterialTheme.colors.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
        }

        Text(
            "Version ${AppConstants.APP_VERSION}",
            style = MaterialTheme.typography.body2,
            maxLines = 1,
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
            title = "Open a repository",
            painter = painterResource(AppIcons.OPEN),
            onClick = onOpenRepository
        )

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = "Clone a repository",
            painter = painterResource(AppIcons.DOWNLOAD),
            onClick = onShowCloneView
        )

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = "Start a local repository",
            painter = painterResource(AppIcons.OPEN),
            onClick = onStartRepository
        )

        Text(
            text = "Additional options",
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        IconTextButton(
            title = "Source code",
            painter = painterResource(AppIcons.CODE),
            onClick = {
                onOpenUrlInBrowser("https://github.com/JetpackDuba/Gitnuro")
            }
        )

        IconTextButton(
            title = "Report a bug",
            painter = painterResource(AppIcons.BUG),
            onClick = {
                onOpenUrlInBrowser("https://github.com/JetpackDuba/Gitnuro/issues")
            }
        )

        IconTextButton(
            title = "Additional information",
            painter = painterResource(AppIcons.INFO),
            onClick = onShowAdditionalInfo
        )

        IconTextButton(
            title = "Settings",
            painter = painterResource(AppIcons.SETTINGS),
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
) {
    Column(
        modifier = Modifier
            .padding(start = 32.dp)
            .width(600.dp)
            .height(400.dp),
    ) {
        var filter by remember {
            mutableStateOf("")
        }

        if (recentlyOpenedRepositories.isEmpty()) {
            Text(
                "Nothing to see here, open a repository first!",
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            AdjustableOutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                hint = "Search for recent repositories",
                trailingIcon = {
                    if (filter.isNotEmpty()) {
                        IconButton(
                            onClick = { filter = "" },
                            modifier = Modifier
                                .size(16.dp)
                                .handOnHover(),
                        ) {
                            Icon(
                                painterResource(AppIcons.CLOSE),
                                contentDescription = null,
                                tint = if (filter.isEmpty()) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground
                            )
                        }
                    }
                }
            )

            val filteredRepositories = remember(filter, recentlyOpenedRepositories) {
                if (filter.isBlank()) {
                    recentlyOpenedRepositories
                } else {
                    recentlyOpenedRepositories.filter { repository ->
                        repository.lowercaseContains(filter)
                    }
                }
            }

            val listState = rememberLazyListState()

            Box (modifier = Modifier.padding(top = 4.dp)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items = filteredRepositories) { repo ->
                        val repoDirName = repo.dirName
                        val repoDirPath = repo.dirPath
                        val hoverInteraction = remember { MutableInteractionSource() }
                        val isHovered by hoverInteraction.collectIsHoveredAsState()

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .fillMaxWidth()
                                .hoverable(hoverInteraction)
                                .handMouseClickable { onOpenKnownRepository(repo) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
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

                                Text(
                                    text = repoDirPath,
                                    style = MaterialTheme.typography.body2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier,
                                    maxLines = 1,
                                    color = MaterialTheme.colors.onBackgroundSecondary
                                )
                            }


                            val buttonAlpha = if (canRepositoriesBeRemoved && isHovered) {
                                1f
                            } else {
                                0f
                            }

                            IconButton(
                                onClick = { onRemoveRepositoryFromRecent(repo) },
                                enabled = canRepositoriesBeRemoved && isHovered,
                                modifier = Modifier.alpha(buttonAlpha)
                                    .size(24.dp)
                                    .handOnHover(),
                            ) {
                                Icon(
                                    painterResource(AppIcons.CLOSE),
                                    contentDescription = "Remove repository from recent",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colors.onBackgroundSecondary
                                )
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
        modifier = modifier.size(width = 280.dp, height = 40.dp),
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

