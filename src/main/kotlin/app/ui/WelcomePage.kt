@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.AppConstants
import app.AppStateManager
import app.extensions.dirName
import app.extensions.dirPath
import app.extensions.openUrlInBrowser
import app.theme.primaryTextColor
import app.theme.secondaryTextColor
import app.ui.dialogs.AppInfoDialog
import app.ui.dialogs.CloneDialog
import app.updates.Update
import app.viewmodels.TabViewModel
import openDirectoryDialog
import openRepositoryDialog


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WelcomePage(
    tabViewModel: TabViewModel,
) {
    val appStateManager = tabViewModel.appStateManager
    var showCloneView by remember { mutableStateOf(false) }
    var showAdditionalInfo by remember { mutableStateOf(false) }
    var newUpdate by remember { mutableStateOf<Update?>(null) }

    LaunchedEffect(Unit) {
        val latestRelease = tabViewModel.latestRelease()

        if (latestRelease != null && latestRelease.appCode > AppConstants.APP_VERSION_CODE) {
            newUpdate = latestRelease
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.align(BiasAlignment(0f, -0.5f))
        ) {
            HomeButtons(
                newUpdate = newUpdate,
                tabViewModel = tabViewModel,
                onShowCloneView = { showCloneView = true },
                onShowAdditionalInfo = { showAdditionalInfo = true },
            )

            RecentRepositories(appStateManager, tabViewModel)
        }

        Text(
            "Version ${AppConstants.APP_VERSION}",
            color = MaterialTheme.colors.primaryTextColor,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        )
    }

    LaunchedEffect(showCloneView) {
        if (showCloneView) {
            tabViewModel.cloneViewModel.reset() // Reset dialog before showing it
        }
    }

    if (showCloneView) {
        CloneDialog(
            tabViewModel.cloneViewModel,
            onClose = {
                showCloneView = false
                tabViewModel.cloneViewModel.reset()
            },
            onOpenRepository = { dir ->
                tabViewModel.openRepository(dir)
            },
        )
    }

    if (showAdditionalInfo) {
        AppInfoDialog(
            onClose = { showAdditionalInfo = false },
        )
    }
}

@Composable
fun HomeButtons(
    newUpdate: Update?,
    tabViewModel: TabViewModel,
    onShowCloneView: () -> Unit,
    onShowAdditionalInfo: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(end = 32.dp),
    ) {
        Text(
            text = "Gitnuro",
            fontSize = 32.sp,
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = "Open a repository",
            painter = painterResource("open.svg"),
            onClick = { openRepositoryDialog(tabViewModel) })

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = "Clone a repository",
            painter = painterResource("download.svg"),
            onClick = onShowCloneView
        )

        ButtonTile(
            modifier = Modifier.padding(bottom = 8.dp),
            title = "Start a local repository",
            painter = painterResource("open.svg"),
            onClick = {
                val dir = openDirectoryDialog()
                if (dir != null) tabViewModel.initLocalRepository(dir)
            }
        )

        Text(
            text = "About Gitnuro",
            fontSize = 18.sp,
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        IconTextButton(
            title = "Source code",
            painter = painterResource("code.svg"),
            onClick = {
                openUrlInBrowser("https://github.com/JetpackDuba/Gitnuro")
            }
        )

        IconTextButton(
            title = "Report a bug",
            painter = painterResource("bug.svg"),
            onClick = {
                openUrlInBrowser("https://github.com/JetpackDuba/Gitnuro/issues")
            }
        )

        IconTextButton(
            title = "Additional information",
            painter = painterResource("info.svg"),
            onClick = onShowAdditionalInfo
        )

        if (newUpdate != null) {
            IconTextButton(
                title = "New update ${newUpdate.appVersion} available ",
                painter = painterResource("grade.svg"),
                iconColor = MaterialTheme.colors.secondary,
                onClick = {
                    openUrlInBrowser(newUpdate.downloadUrl)
                }
            )
        }
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
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 48.dp, bottom = 8.dp),
            color = MaterialTheme.colors.primaryTextColor,
        )

        if (latestOpenedRepositoriesPaths.isEmpty()) {
            Text(
                "Nothing to see here, open a repository first!",
                color = MaterialTheme.colors.secondaryTextColor,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            LazyColumn {
                items(items = latestOpenedRepositoriesPaths) { repo ->
                    val repoDirName = repo.dirName
                    val repoDirPath = repo.dirPath

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                tabViewModel.openRepository(repo)
                            }
                        ) {
                            Text(
                                text = repoDirName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colors.primary,
                            )
                        }

                        Text(
                            text = repoDirPath,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp),
                            color = MaterialTheme.colors.secondaryTextColor
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
        modifier = modifier.size(width = 280.dp, height = 56.dp)
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
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primary),
            )

            Text(
                text = title,
            )
        }
    }
}

@Composable
fun IconTextButton(
    modifier: Modifier = Modifier,
    title: String,
    painter: Painter,
    iconColor: Color = MaterialTheme.colors.primary,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.size(width = 280.dp, height = 40.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.padding(end = 8.dp).size(24.dp),
                painter = painter,
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconColor),
            )

            Text(
                text = title,
            )
        }
    }
}

