@file:OptIn(ExperimentalComposeUiApi::class)

package app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.zIndex
import app.di.DaggerAppComponent
import app.git.GitManager
import app.theme.AppTheme
import app.ui.AppTab
import app.ui.components.RepositoriesTabPanel
import app.ui.components.TabInformation
import javax.inject.Inject
import javax.inject.Provider

class Main {
    private val appComponent = DaggerAppComponent.create()

    @Inject
    lateinit var gitManagerProvider: Provider<GitManager>

    @Inject
    lateinit var appStateManager: AppStateManager

    init {
        appComponent.inject(this)

        appStateManager.loadRepositoriesTabs()
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
                    size = DpSize(1280.dp, 720.dp)
                )
            ) {
                AppTheme {
                    Box {
                        AppTabs()
                    }
                }
            }
        }
    }


    @Composable
    fun AppTabs() {
        val tabs = remember {
            val repositoriesSavedTabs = appStateManager.openRepositoriesPathsTabs
            var repoTabs = repositoriesSavedTabs.map { repositoryTab ->
                newAppTab(
                    key = repositoryTab.key,
                    path = repositoryTab.value
                )
            }

            if (repoTabs.isEmpty()) {
                repoTabs = listOf(
                    newAppTab()
                )
            }

            mutableStateOf(repoTabs)
        }

        val selectedTabKey = remember { mutableStateOf(0) }

        Column(
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            Tabs(
                tabs = tabs,
                selectedTabKey = selectedTabKey,
            )

            TabsContent(tabs.value, selectedTabKey.value)
        }
    }

    @Composable
    fun Tabs(
        tabs: MutableState<List<TabInformation>>,
        selectedTabKey: MutableState<Int>,
    ) {
        Row(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepositoriesTabPanel(
                modifier = Modifier
                    .weight(1f),
                tabs = tabs.value,
                selectedTabKey = selectedTabKey.value,
                onTabSelected = { newSelectedTabKey ->
                    selectedTabKey.value = newSelectedTabKey
                },
                newTabContent = { key ->
                    newAppTab(
                        key = key
                    )
                },
                onTabsUpdated = { tabInformationList ->
                    tabs.value = tabInformationList
                },
                onTabClosed = { key ->
                    appStateManager.repositoryTabRemoved(key)
                }
            )
            IconButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp),
                onClick = {}
            ) {
                Icon(
                    painter = painterResource("settings.svg"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }

    private fun newAppTab(
        key: Int = 0,
        tabName: MutableState<String> = mutableStateOf("New tab"),
        path: String? = null,
    ): TabInformation {

        return TabInformation(
            title = tabName,
            key = key
        ) {
            val gitManager = remember { gitManagerProvider.get() }
            gitManager.onRepositoryChanged = { path ->
                if (path == null) {
                    appStateManager.repositoryTabRemoved(key)
                } else
                    appStateManager.repositoryTabChanged(key, path)
            }

            AppTab(gitManager, path, tabName)
        }
    }
}

@Composable
private fun TabsContent(tabs: List<TabInformation>, selectedTabKey: Int) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        items(items = tabs, key = { it.key }) {
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

@Composable
fun LoadingRepository() {
    Box { }
}
