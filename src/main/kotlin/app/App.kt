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
import app.theme.AppTheme
import app.ui.components.RepositoriesTabPanel
import app.ui.components.TabInformation
import app.ui.dialogs.SettingsDialog
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class App {
    private val appComponent = DaggerAppComponent.create()

    @Inject
    lateinit var appStateManager: AppStateManager

    @Inject
    lateinit var appPreferences: AppPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        appComponent.inject(this)
        println("AppStateManagerReference $appStateManager")
    }

    private val tabsFlow = MutableStateFlow<List<TabInformation>>(emptyList())

    fun start(){
        appStateManager.loadRepositoriesTabs()
        loadTabs()

        application {
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

                    AppTheme(theme = theme) {
                        Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                            AppTabs(
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
            } else {
                appScope.cancel("Closing app")
                this.exitApplication()
            }
        }
    }

    private fun loadTabs() {
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

        tabsFlow.value = repoTabs

        println("After reading prefs, got ${tabsFlow.value.count()} tabs")
    }


    @Composable
    fun AppTabs(
        onOpenSettings: () -> Unit,
    ) {
        val tabs by tabsFlow.collectAsState()
        val tabsInformationList = tabs.sortedBy { it.key }

        println("Tabs count ${tabs.count()}")

        val selectedTabKey = remember { mutableStateOf(0) }

        println("Selected tab key: ${selectedTabKey.value}")

        Column(
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            Tabs(
                tabsInformationList = tabsInformationList,
                selectedTabKey = selectedTabKey,
                onOpenSettings = onOpenSettings,
                onAddedTab = { tabInfo ->
                    addTab(tabInfo)
                },
                onRemoveTab = { key ->
                    removeTab(key)
                }
            )

            TabsContent(tabsInformationList, selectedTabKey.value)
        }
    }

    private fun removeTab(key: Int) = appScope.launch(Dispatchers.IO) {
        // Stop any running jobs
        val tabs = tabsFlow.value
        val tabToRemove = tabs.firstOrNull { it.key == key } ?: return@launch
        tabToRemove.tabViewModel.dispose()

        // Remove tab from persistent tabs storage
        appStateManager.repositoryTabRemoved(key)

        // Remove from tabs flow
        tabsFlow.value = tabsFlow.value.filter { tab -> tab.key != key }
    }

    fun addTab(tabInformation: TabInformation) = appScope.launch(Dispatchers.IO) {
        tabsFlow.value = tabsFlow.value.toMutableList().apply { add(tabInformation) }
    }

    @Composable
    fun Tabs(
        selectedTabKey: MutableState<Int>,
        onOpenSettings: () -> Unit,
        tabsInformationList: List<TabInformation>,
        onAddedTab: (TabInformation) -> Unit,
        onRemoveTab: (Int) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 0.dp, start = 4.dp, end = 4.dp)
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

                    onAddedTab(newAppTab)
                    newAppTab
                },
                onTabClosed = onRemoveTab
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
            tabName = tabName,
            key = key,
            path = path,
            appComponent = appComponent,
        )
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