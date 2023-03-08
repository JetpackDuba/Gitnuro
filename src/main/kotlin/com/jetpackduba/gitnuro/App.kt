@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jetpackduba.gitnuro.di.DaggerAppComponent
import com.jetpackduba.gitnuro.extensions.preferenceValue
import com.jetpackduba.gitnuro.extensions.systemSeparator
import com.jetpackduba.gitnuro.extensions.toWindowPlacement
import com.jetpackduba.gitnuro.git.AppGpgSigner
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.theme.AppTheme
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.AppTab
import com.jetpackduba.gitnuro.ui.components.RepositoriesTabPanel
import com.jetpackduba.gitnuro.ui.components.TabInformation
import com.jetpackduba.gitnuro.ui.components.emptyTabInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.GpgSigner
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject

private const val TAG = "App"

val LocalTabScope = compositionLocalOf { emptyTabInformation() }

class App {
    private val appComponent = DaggerAppComponent.create()

    @Inject
    lateinit var appStateManager: AppStateManager

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var appGpgSigner: AppGpgSigner

    init {
        appComponent.inject(this)
    }

    private val tabsFlow = MutableStateFlow<List<TabInformation>>(emptyList())

    fun start(args: Array<String>) {
        val windowPlacement = appSettings.windowPlacement.toWindowPlacement
        val dirToOpen = getDirToOpen(args)
        var defaultSelectedTabKey = 0

        appStateManager.loadRepositoriesTabs()

        try {
            if (appSettings.theme == Theme.CUSTOM) {
                appSettings.loadCustomTheme()
            }
        } catch (ex: Exception) {
            printLog(TAG, "Failed to load custom theme")
            ex.printStackTrace()
        }

        loadTabs()

        GpgSigner.setDefault(appGpgSigner)

        if (dirToOpen != null)
            defaultSelectedTabKey = addDirTab(dirToOpen)

        application {
            var isOpen by remember { mutableStateOf(true) }
            val theme by appSettings.themeState.collectAsState()
            val customTheme by appSettings.customThemeFlow.collectAsState()
            val scale by appSettings.scaleUiFlow.collectAsState()

            val windowState = rememberWindowState(
                placement = windowPlacement,
                size = DpSize(1280.dp, 720.dp)
            )

            // Save window state for next time the Window is started
            appSettings.windowPlacement = windowState.placement.preferenceValue

            if (isOpen) {
                Window(
                    title = AppConstants.APP_NAME,
                    onCloseRequest = {
                        isOpen = false
                    },
                    state = windowState,
                    icon = painterResource("logo.svg"),
                ) {
                    val density = if (scale != -1f) {
                        arrayOf(LocalDensity provides Density(scale, 1f))
                    } else
                        emptyArray()

                    CompositionLocalProvider(values = density) {
                        AppTheme(
                            selectedTheme = theme,
                            customTheme = customTheme,
                        ) {
                            Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                                AppTabs(defaultSelectedTabKey)
                            }
                        }
                    }
                }
            } else {
                appStateManager.cancelCoroutines()
                this.exitApplication()
            }

        }
    }

    private fun addDirTab(dirToOpen: File): Int {
        var defaultSelectedTabKey = 0

        tabsFlow.update {
            val newList = it.toMutableList()
            val absolutePath = dirToOpen.normalize().absolutePath
                .removeSuffix(systemSeparator)
                .removeSuffix("$systemSeparator.git")
            val newKey = it.count()

            val existingIndex =
                newList.indexOfFirst { repo -> repo.path?.removeSuffix(systemSeparator) == absolutePath }

            defaultSelectedTabKey = if (existingIndex == -1) {
                newList.add(newAppTab(key = newKey, path = absolutePath))
                newKey
            } else {
                existingIndex
            }

            newList
        }

        return defaultSelectedTabKey
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
    fun AppTabs(defaultSelectedTabKey: Int) {
        val tabs by tabsFlow.collectAsState()
        val tabsInformationList = tabs.sortedBy { it.key }
        val selectedTabKey = remember { mutableStateOf(defaultSelectedTabKey) }

        Column(
            modifier = Modifier.background(MaterialTheme.colors.background)
        ) {
            Tabs(
                tabsInformationList = tabsInformationList,
                selectedTabKey = selectedTabKey,
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

    private fun removeTab(key: Int) = appStateManager.appScope.launch(Dispatchers.IO) {
        // Stop any running jobs
        val tabs = tabsFlow.value
        val tabToRemove = tabs.firstOrNull { it.key == key } ?: return@launch
        tabToRemove.tabViewModel.dispose()

        // Remove tab from persistent tabs storage
        appStateManager.repositoryTabRemoved(key)

        // Remove from tabs flow
        tabsFlow.value = tabsFlow.value.filter { tab -> tab.key != key }
    }

    fun addTab(tabInformation: TabInformation) = appStateManager.appScope.launch(Dispatchers.IO) {
        tabsFlow.value = tabsFlow.value.toMutableList().apply { add(tabInformation) }
    }

    @Composable
    fun Tabs(
        selectedTabKey: MutableState<Int>,
        tabsInformationList: List<TabInformation>,
        onAddedTab: (TabInformation) -> Unit,
        onRemoveTab: (Int) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepositoriesTabPanel(
                tabs = tabsInformationList,
                selectedTabKey = selectedTabKey.value,
                onTabSelected = { newSelectedTabKey ->
                    selectedTabKey.value = newSelectedTabKey
                },
                onTabClosed = onRemoveTab
            ) { key ->
                val newAppTab = newAppTab(
                    key = key
                )

                onAddedTab(newAppTab)
                newAppTab
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

    fun getDirToOpen(args: Array<String>): File? {
        if (args.isNotEmpty()) {
            val repoToOpen = args.first()
            val path = Paths.get(repoToOpen)

            val repoDir = if (!path.isAbsolute)
                File(System.getProperty("user.dir"), repoToOpen)
            else
                path.toFile()

            return if (repoDir.isDirectory)
                repoDir
            else
                null
        }

        return null
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
            val density = arrayOf(LocalTabScope provides selectedTab)


            CompositionLocalProvider(values = density) {
                AppTab(selectedTab.tabViewModel)
            }
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
            Text("Opening repository", fontSize = 36.sp, color = MaterialTheme.colors.onBackground)
            Text(repoPath, fontSize = 24.sp, color = MaterialTheme.colors.onBackgroundSecondary)
        }
    }
}

object AboutIcon : Painter() {
    override val intrinsicSize = Size(256f, 256f)

    override fun DrawScope.onDraw() {
        drawOval(Color(0xFFFFA500))
    }
}