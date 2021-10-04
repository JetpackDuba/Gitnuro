import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import git.GitManager
import git.RepositorySelectionStatus
import theme.*
import ui.RepositoryOpenPage
import ui.WelcomePage
import ui.components.RepositoriesTabPanel
import ui.components.TabInformation
import javax.swing.JFileChooser

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    var isOpen by remember { mutableStateOf(true) }
    if (isOpen) {
        Window(
            title = "Gitnuro",
            onCloseRequest = {
                isOpen = false
            },
            state = rememberWindowState(placement = WindowPlacement.Maximized, size = WindowSize(1280.dp, 720.dp))
        ) {
            GitnuroTheme {
                val tabs = remember {
                    val tabName = mutableStateOf("New tab")
                    mutableStateOf(
                        listOf(
                            TabInformation(tabName, key = 0) {
                                Gitnuro(false, tabName)
                            },
                        )
                    )
                }

                var selectedTabKey by remember { mutableStateOf(0) }

                Column {
                    RepositoriesTabPanel(
                        modifier = Modifier
                            .padding(top = 4.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)
                            .fillMaxWidth(),
                        tabs = tabs.value,
                        selectedTabKey = selectedTabKey,
                        onTabSelected = { newSelectedTabKey ->
                            selectedTabKey = newSelectedTabKey
                        },
                        newTabContent = { tabName ->
                            Gitnuro(true, tabName)
                        },
                        onTabsUpdated = { tabInformationList ->
                            tabs.value = tabInformationList
                        }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        items(items = tabs.value, key = { it.key }) {
                            val isItemSelected = it.key == selectedTabKey

                            var tabMod: Modifier = if (!isItemSelected)
                                Modifier.size(0.dp)
                            else
                                Modifier
                                    .fillParentMaxSize()

                            tabMod = tabMod.background(MaterialTheme.colors.primary)
                                .alpha(if (isItemSelected) 1f else -1f)
                                .zIndex(if (isItemSelected) 1f else -1f)
                            Box(
                                modifier = tabMod,
                            ) {
                                it.content()
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun Gitnuro(isNewTab: Boolean, tabName: MutableState<String>) {
    val gitManager = remember {
        GitManager().apply {
            if (!isNewTab)
                loadLatestOpenedRepository()
        }
    }

    val repositorySelectionStatus by gitManager.repositorySelectionStatus.collectAsState()

    if (repositorySelectionStatus is RepositorySelectionStatus.Open) {
        tabName.value = gitManager.repositoryName
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        GMenu(
            onRepositoryOpen = {
                openRepositoryDialog(gitManager = gitManager)
            },
            onPull = { gitManager.pull() },
            onPush = { gitManager.push() },
            onStash = { gitManager.stash() },
            onPopStash = { gitManager.popStash() },
            isRepositoryOpen = repositorySelectionStatus is RepositorySelectionStatus.Open
        )

        Crossfade(targetState = repositorySelectionStatus) {

            @Suppress("UnnecessaryVariable") // Don't inline it because smart cast won't work
            when (repositorySelectionStatus) {
                RepositorySelectionStatus.None -> {
                    WelcomePage()
                }
                RepositorySelectionStatus.Loading -> {
                    LoadingRepository()
                }
                is RepositorySelectionStatus.Open -> {
                    RepositoryOpenPage(gitManager = gitManager)
                }
            }
        }
    }


}

@Composable
fun LoadingRepository() {
    Box { }

}


@Composable
fun GMenu(
    onRepositoryOpen: () -> Unit,
    isRepositoryOpen: Boolean,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onStash: () -> Unit,
    onPopStash: () -> Unit,
) {
    val openHovering = remember { mutableStateOf(false) }
    val pullHovering = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        MenuButton(
            title = "Open",
            hovering = openHovering,
            icon = painterResource("open.svg"),
            onClick = {
                openHovering.value = false // Without this, the hover would be kept because of the newly opened dialog
                onRepositoryOpen()
            }
        )
        MenuButton(
            title = "Pull",
            hovering = pullHovering,
            icon = painterResource("download.svg"),
            onClick = {
                pullHovering.value = false
                onPull()
            },
            enabled = isRepositoryOpen,
        )
        MenuButton(
            title = "Push",
            icon = painterResource("upload.svg"),
            onClick = onPush,
            enabled = isRepositoryOpen,
        )
        MenuButton(
            title = "Stash",
            icon = painterResource("stash.svg"),
            onClick = onStash,
            enabled = isRepositoryOpen,
        )
        MenuButton(
            title = "Apply",
            icon = painterResource("apply_stash.svg"),
            onClick = onPopStash,
            enabled = isRepositoryOpen,
        )
    }
}

@Composable
fun MenuButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hovering: MutableState<Boolean> = remember { mutableStateOf(false) },
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val backgroundColor = if (hovering.value)
        MaterialTheme.colors.primary.copy(alpha = 0.15F)
    else
        Color.Transparent

    val iconColor = if (enabled) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.secondaryVariant
    }

    TextButton(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .pointerMoveFilter(
                onEnter = {
                    hovering.value = true
                    false
                },
                onExit = {
                    hovering.value = false
                    false
                }
            ),
        enabled = enabled,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            disabledBackgroundColor = Color.Transparent
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = icon,
                contentDescription = title,
                modifier = Modifier
                    .padding(4.dp)
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
