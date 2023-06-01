@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import com.jetpackduba.gitnuro.extensions.dirName
import com.jetpackduba.gitnuro.extensions.dirPath
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.ui.dialogs.AppInfoDialog
import com.jetpackduba.gitnuro.viewmodels.TabViewModel


@Composable
fun WelcomePage(
    tabViewModel: TabViewModel,
    onShowCloneDialog: () -> Unit,
    onShowSettings: () -> Unit,
) {
    val appStateManager = tabViewModel.appStateManager
    var showAdditionalInfo by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = BiasAlignment.Vertical(-0.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
                .weight(1f),
        ) {
            HomeButtons(
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
                onShowCloneView = onShowCloneDialog,
                onShowAdditionalInfo = { showAdditionalInfo = true },
                onShowSettings = onShowSettings,
                onOpenUrlInBrowser = { url -> tabViewModel.openUrlInBrowser(url) }
            )

            RecentRepositories(appStateManager, tabViewModel)
        }
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colors.primaryVariant.copy(alpha = 0.2f))
        )

        BottomInfoBar(tabViewModel)
    }

    if (showAdditionalInfo) {
        AppInfoDialog(
            onClose = { showAdditionalInfo = false },
            onOpenUrlInBrowser = { url -> tabViewModel.openUrlInBrowser(url) }
        )
    }
}

@Composable
private fun BottomInfoBar(tabViewModel: TabViewModel) {
    val newUpdate = tabViewModel.hasUpdates.collectAsState().value

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
                onClick = { tabViewModel.openUrlInBrowser(newUpdate.downloadUrl) },
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
    Column(
        modifier = Modifier.padding(end = 32.dp),
    ) {
        Text(
            text = AppConstants.APP_NAME,
            style = MaterialTheme.typography.h1,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 16.dp),
        )

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
fun RecentRepositories(appStateManager: AppStateManager, tabViewModel: TabViewModel) {
    Column(
        modifier = Modifier
            .padding(start = 32.dp),
    ) {
        val latestOpenedRepositoriesPaths = appStateManager.latestOpenedRepositoriesPaths
        Text(
            text = "Recent",
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(top = 48.dp, bottom = 4.dp),
        )

        if (latestOpenedRepositoriesPaths.isEmpty()) {
            Text(
                "Nothing to see here, open a repository first!",
                color = MaterialTheme.colors.onBackgroundSecondary,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn {
                items(items = latestOpenedRepositoriesPaths) { repo ->
                    val repoDirName = repo.dirName
                    val repoDirPath = repo.dirPath

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .handMouseClickable {
                                    tabViewModel.openRepository(repo)
                                },
                        ) {
                            Text(
                                text = repoDirName,
                                style = MaterialTheme.typography.body1,
                                maxLines = 1,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colors.primaryVariant,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .widthIn(max = 600.dp),
                            )
                        }

                        Text(
                            text = repoDirPath,
                            style = MaterialTheme.typography.body1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .widthIn(max = 600.dp),
                            maxLines = 1,
                            color = MaterialTheme.colors.onBackgroundSecondary
                        )
                    }
                }
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

