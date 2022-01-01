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
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import app.ui.dialogs.SettingsDialog
import javax.inject.Inject
import javax.inject.Provider

class Main {
    private val appComponent = DaggerAppComponent.create()

    @Inject
    lateinit var gitManagerProvider: Provider<GitManager>

    @Inject
    lateinit var appStateManager: AppStateManager

    @Inject
    lateinit var appPreferences: AppPreferences

    init {
        appComponent.inject(this)

        appStateManager.loadRepositoriesTabs()
    }

    fun start() = application {
        var isOpen by remember { mutableStateOf(true) }
        val theme by appPreferences.themeState.collectAsState()
        if (isOpen) {
            Window(
                title = "Gitnuro",
                onCloseRequest = {
                    isOpen = false
                },
                state = rememberWindowState(
                    placement = WindowPlacement.Maximized,
                    size = DpSize(1280.dp, 720.dp)
                ),
                icon = painterResource("logo.svg"),
            ) {
                var showSettingsDialog by remember { mutableStateOf(false) }
                val tabs = mutableStateMapOf<Int, TabInformation>()

                AppTheme(theme = theme) {
                    Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                        AppTabs(
                            tabs = tabs,
                            onOpenSettings = {
                                showSettingsDialog = true
                            }
                        )
                    }

                    if (showSettingsDialog) {
                        SettingsDialog(
                            appPreferences = appPreferences,
                            onDismiss = { showSettingsDialog = false }
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun AppTabs(
        tabs: SnapshotStateMap<Int, TabInformation>,
        onOpenSettings: () -> Unit,
    ) {

        val tabsInformationList = tabs.map { it.value }.sortedBy { it.key }

        println("Tabs count ${tabs.count()}")

        LaunchedEffect(Unit) {
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

            repoTabs.forEach {
                tabs[it.key] = it
            } // Store list of tabs in the map

            println("After reading prefs, got ${tabs.count()} tabs")
        }

        val selectedTabKey = remember { mutableStateOf(0) }

        println("Selected tab key: ${selectedTabKey.value}")

        Column(
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            Tabs(
                tabs = tabs,
                tabsInformationList = tabsInformationList,
                selectedTabKey = selectedTabKey,
                onOpenSettings = onOpenSettings
            )

            TabsContent(tabsInformationList, selectedTabKey.value)
        }
    }

    @Composable
    fun Tabs(
        tabs: SnapshotStateMap<Int, TabInformation>,
        selectedTabKey: MutableState<Int>,
        onOpenSettings: () -> Unit,
        tabsInformationList: List<TabInformation>,
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
                tabs = tabsInformationList,
                selectedTabKey = selectedTabKey.value,
                onTabSelected = { newSelectedTabKey ->
                    println("New selected tab key $newSelectedTabKey")
                    selectedTabKey.value = newSelectedTabKey
                },
                newTabContent = { key ->
                    val newAppTab = newAppTab(
                        key = key
                    )

                    tabs[key] = newAppTab

                    newAppTab
                },
                onTabClosed = { key ->
                    appStateManager.repositoryTabRemoved(key)
                    tabs.remove(key)
                }
            )
            IconButton(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(24.dp),
                onClick = onOpenSettings
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