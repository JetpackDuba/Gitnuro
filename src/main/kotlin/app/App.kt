@file:OptIn(ExperimentalComposeUiApi::class)

package app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.di.DaggerAppComponent
import app.extensions.preferenceValue
import app.extensions.toWindowPlacement
import app.preferences.AppPreferences
import app.theme.AppTheme
import app.theme.primaryTextColor
import app.theme.secondaryTextColor
import app.ui.AppTab
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

    init {
        appComponent.inject(this)
    }

    private val tabsFlow = MutableStateFlow<List<TabInformation>>(emptyList())

    fun start() {
        val windowPlacement = appPreferences.windowPlacement.toWindowPlacement

        appStateManager.loadRepositoriesTabs()
        appPreferences.loadCustomTheme()
        loadTabs()

        application {
            var isOpen by remember { mutableStateOf(true) }
            val theme by appPreferences.themeState.collectAsState()
            val customTheme by appPreferences.customThemeFlow.collectAsState()

            val windowState = rememberWindowState(
                placement = windowPlacement,
                size = DpSize(1280.dp, 720.dp)
            )

            // Save window state for next time the Window is started
            appPreferences.windowPlacement = windowState.placement.preferenceValue

            if (isOpen) {
                Window(
                    title = AppConstants.APP_NAME,
                    onCloseRequest = {
                        isOpen = false
                    },
                    state = windowState,
                    icon = painterResource("logo.svg"),
                ) {
                    var showSettingsDialog by remember { mutableStateOf(false) }

                    AppTheme(
                        selectedTheme = theme,
                        customTheme = customTheme,
                    ) {
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
                appStateManager.cancelCoroutines()
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
        val selectedTabKey = remember { mutableStateOf(0) }

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

    private fun removeTab(key: Int) = appStateManager.appStateScope.launch(Dispatchers.IO) {
        // Stop any running jobs
        val tabs = tabsFlow.value
        val tabToRemove = tabs.firstOrNull { it.key == key } ?: return@launch
        tabToRemove.tabViewModel.dispose()

        // Remove tab from persistent tabs storage
        appStateManager.repositoryTabRemoved(key)

        // Remove from tabs flow
        tabsFlow.value = tabsFlow.value.filter { tab -> tab.key != key }
    }

    fun addTab(tabInformation: TabInformation) = appStateManager.appStateScope.launch(Dispatchers.IO) {
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
                    .size(24.dp)
                    .pointerHoverIcon(PointerIconDefaults.Hand),
                onClick = onOpenSettings
            ) {
                Icon(
                    painter = painterResource("settings.svg"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colors.primaryVariant,
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
    val selectedTab = tabs.firstOrNull { it.key == selectedTabKey }

    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize(),
    ) {
        if (selectedTab != null) {
            AppTab(selectedTab.tabViewModel)
        }
    }
}

@Composable
fun LoadingRepository(repoPath: String) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Opening repository", fontSize = 36.sp, color = MaterialTheme.colors.primaryTextColor)
            Text(repoPath, fontSize = 24.sp, color = MaterialTheme.colors.secondaryTextColor)
        }
    }
}