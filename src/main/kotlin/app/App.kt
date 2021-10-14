package app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import app.di.DaggerAppComponent
import app.git.GitManager
import app.git.RepositorySelectionStatus
import app.theme.AppTheme
import app.ui.RepositoryOpenPage
import app.ui.WelcomePage
import app.ui.components.RepositoriesTabPanel
import app.ui.components.TabInformation
import javax.inject.Inject
import javax.inject.Provider

class Main {
    val appComponent = DaggerAppComponent.create()

    @Inject
    lateinit var gitManagerProvider: Provider<GitManager>


    init {
        appComponent.inject(this)
    }

    fun start() = application {
        var isOpen by remember { mutableStateOf(true) }
        if (isOpen) {
            Window(
                title = "Gitnuro",
                onCloseRequest = {
                    isOpen = false
                },
                state = rememberWindowState(
                    placement = WindowPlacement.Maximized,
                    size = WindowSize(1280.dp, 720.dp)
                )
            ) {
                AppTheme {
                    val tabs = remember {
                        val tabName = mutableStateOf("New tab")
                        mutableStateOf(
                            listOf(
                                TabInformation(tabName, key = 0) {
                                    val gitManager = remember { gitManagerProvider.get() }
                                    Gitnuro(gitManager, false, tabName)
                                },
                            )
                        )
                    }

                    var selectedTabKey by remember { mutableStateOf(0) }
                    Column(
                        modifier =
                        Modifier.background(MaterialTheme.colors.surface)
                    ) {
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
                                val gitManager = remember { gitManagerProvider.get() }
                                Gitnuro(gitManager, true, tabName)
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
                                    it.content(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Gitnuro(gitManager: GitManager, isNewTab: Boolean, tabName: MutableState<String>) {
    LaunchedEffect(gitManager) {
        if (!isNewTab)
            gitManager.loadLatestOpenedRepository()
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
        Crossfade(targetState = repositorySelectionStatus) {

            @Suppress("UnnecessaryVariable") // Don't inline it because smart cast won't work
            when (repositorySelectionStatus) {
                RepositorySelectionStatus.None -> {
                    WelcomePage(gitManager = gitManager)
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
