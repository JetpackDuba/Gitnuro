@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.jetpackduba.gitnuro.di.DaggerAppComponent
import com.jetpackduba.gitnuro.extensions.preferenceValue
import com.jetpackduba.gitnuro.extensions.toWindowPlacement
import com.jetpackduba.gitnuro.git.AppGpgSigner
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.managers.TempFilesManager
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.system.systemSeparator
import com.jetpackduba.gitnuro.theme.AppTheme
import com.jetpackduba.gitnuro.theme.Theme
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.AppTab
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.ui.components.RepositoriesTabPanel
import com.jetpackduba.gitnuro.ui.components.TabInformation
import com.jetpackduba.gitnuro.ui.components.emptyTabInformation
import com.jetpackduba.gitnuro.ui.context_menu.AppPopupMenu
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

    @Inject
    lateinit var appEnvInfo: AppEnvInfo

    @Inject
    lateinit var tabsManager: TabsManager

    @Inject
    lateinit var tempFilesManager: TempFilesManager

    init {
        appComponent.inject(this)
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun start(args: Array<String>) {
        tabsManager.appComponent = this.appComponent
        val windowPlacement = appSettings.windowPlacement.toWindowPlacement
        val dirToOpen = getDirToOpen(args)

        appEnvInfo.isFlatpak = args.contains("--flatpak")
        appStateManager.loadRepositoriesTabs()

        try {
            if (appSettings.theme == Theme.CUSTOM) {
                appSettings.loadCustomTheme()
            }
        } catch (ex: Exception) {
            printError(TAG, "Failed to load custom theme")
            ex.printStackTrace()
        }

        tabsManager.loadPersistedTabs()

        GpgSigner.setDefault(appGpgSigner)

        if (dirToOpen != null)
            addDirTab(dirToOpen)

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
                    title = System.getenv("title") ?: AppConstants.APP_NAME,
                    onCloseRequest = {
                        isOpen = false
                    },
                    state = windowState,
                    icon = painterResource(AppIcons.LOGO),
                ) {
                    val compositionValues: MutableList<ProvidedValue<*>> = mutableListOf(LocalTextContextMenu provides AppPopupMenu())

                    if (scale != -1f) {
                        compositionValues.add(LocalDensity provides Density(scale, 1f))
                    }

                    CompositionLocalProvider(
                        values = compositionValues.toTypedArray()
                    ) {
                        AppTheme(
                            selectedTheme = theme,
                            customTheme = customTheme,
                        ) {
                            Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                                AppTabs()
                            }
                        }
                    }
                }
            } else {
                tempFilesManager.clearAll()
                appStateManager.cancelCoroutines()
                this.exitApplication()
            }

        }
    }

    private fun addDirTab(dirToOpen: File) {
        val absolutePath = dirToOpen.normalize().absolutePath
            .removeSuffix(systemSeparator)
            .removeSuffix("$systemSeparator.git")

        tabsManager.addNewTabFromPath(absolutePath, true)
    }


    @Composable
    fun AppTabs() {
        val tabs by tabsManager.tabsFlow.collectAsState()
        val currentTab = tabsManager.currentTab.collectAsState().value

        if (currentTab != null) {
            Column(
                modifier = Modifier.background(MaterialTheme.colors.background)
            ) {
                Tabs(
                    tabsInformationList = tabs,
                    currentTab = currentTab,
                    onAddedTab = {
                        tabsManager.addNewEmptyTab()
                    },
                    onCloseTab = { tab ->
                        tabsManager.closeTab(tab)
                    }
                )

                TabContent(currentTab)
            }
        }
    }

    @Composable
    fun Tabs(
        tabsInformationList: List<TabInformation>,
        currentTab: TabInformation?,
        onAddedTab: () -> Unit,
        onCloseTab: (TabInformation) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RepositoriesTabPanel(
                tabs = tabsInformationList,
                currentTab = currentTab,
                onTabSelected = { selectedTab ->
                    tabsManager.selectTab(selectedTab)
                },
                onTabClosed = onCloseTab,
                onAddNewTab = onAddedTab,
                tabsHeight = 40.dp,
            )
        }
    }

    private fun getDirToOpen(args: Array<String>): File? {
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
private fun TabContent(currentTab: TabInformation?) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize(),
    ) {
        if (currentTab != null) {
            val tabScope = arrayOf(LocalTabScope provides currentTab)

            CompositionLocalProvider(values = tabScope) {
                AppTab(currentTab.tabViewModel)
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
