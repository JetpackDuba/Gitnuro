@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.LocalTextContextMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigation3.runtime.NavKey
import com.jetpackduba.gitnuro.app.generated.resources.Res
import com.jetpackduba.gitnuro.app.generated.resources.logo
import com.jetpackduba.gitnuro.avatarproviders.GravatarAvatarProvider
import com.jetpackduba.gitnuro.avatarproviders.NoneAvatarProvider
import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.data.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.domain.TempFilesManager
import com.jetpackduba.gitnuro.domain.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.domain.git.signers.AppGpgSigner
import com.jetpackduba.gitnuro.domain.git.signers.SshSigner
import com.jetpackduba.gitnuro.domain.models.AvatarProviderType
import com.jetpackduba.gitnuro.domain.models.DateTimeFormat
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.extensions.preferenceValue
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.lfs.AppLfsFactory
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.theme.AppTheme
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.AppTab
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.ui.components.RepositoriesTabPanel
import com.jetpackduba.gitnuro.ui.components.TabInformation
import com.jetpackduba.gitnuro.ui.components.TabInformation.Companion.NEW_TAB_DEFAULT_NAME
import com.jetpackduba.gitnuro.ui.context_menu.AppPopupMenu
import org.eclipse.jgit.lib.GpgConfig
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Signers
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.util.LfsFactory
import org.jetbrains.compose.resources.painterResource
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import javax.inject.Inject

private const val TAG = "App"
private const val MAX_CHARS_CURRENT_TAB_NAME = 250

sealed interface Screen : NavKey {
    data object Welcome : Screen
    data object Clone : Screen
    data object RepositoryLoading : Screen
    data object RepositoryOpen : Screen
    data object Settings : Screen
    data object CloneRepository : Screen
    data class BranchRename(val ref: Ref) : Screen
    data class BranchChangeUpstream(val ref: Ref) : Screen
    data class BranchCreate(val targetCommit: RevCommit?) : Screen
    data class TagCreate(val targetCommit: RevCommit) : Screen
    data class BranchReset(val targetCommit: RevCommit) : Screen
    data class AddEditRemote(val remote: Remote) : Screen
    data class Error(val error: com.jetpackduba.gitnuro.domain.models.Error) : Screen
    data object SubmoduleAdd : Screen
    data object HttpCredentials : Screen
    data object SshCredentials : Screen
    data class GpgCredentials(val credentialsRequest: CredentialsRequest.GpgCredentialsRequest) : Screen
    data object LfsCredentials : Screen
    data object QuickActions : Screen
    data object SignOffData : Screen
}


class App @Inject constructor(
    private val appStateManager: AppStateManager,
    private val appSettingsRepository: AppSettingsRepository,
    private val appEnvInfo: AppEnvInfo,
    private val tabsManager: TabsManager,
    private val tempFilesManager: TempFilesManager,
    private val logsRepository: LogsRepository,
    private val gpgSigner: AppGpgSigner,
    private val sshSigner: SshSigner,
    private val lfsFactory: AppLfsFactory,
) {
    @OptIn(ExperimentalFoundationApi::class)
    fun start(args: Array<String>) {
        initNativeDependencies()
        logsRepository.initLogging()
        initProxySettings()

        Signers.set(GpgConfig.GpgFormat.OPENPGP, gpgSigner)
        Signers.set(GpgConfig.GpgFormat.SSH, sshSigner)

        val windowPlacement = WindowPlacement.Maximized //appSettingsRepository.windowPlacement.toWindowPlacement
        val dirToOpen = getDirToOpen(args)

        appEnvInfo.isFlatpak = args.contains("--flatpak")
        appStateManager.loadRepositoriesTabs()

        try {
            /* if (appSettingsRepository.theme == Theme.CUSTOM) {
                 appSettingsRepository.loadCustomTheme()
             }*/
        } catch (ex: Exception) {
            printError(TAG, "Failed to load custom theme")
            ex.printStackTrace()
        }

        tabsManager.loadPersistedTabs()
        LfsFactory.setInstance(lfsFactory)

        if (dirToOpen != null)
            addDirTab(dirToOpen)

        application {
            var isOpen by remember { mutableStateOf(true) }
            val theme by appSettingsRepository.themeState.collectAsState()
            val customTheme = null //by appSettingsRepository.customThemeFlow.collectAsState()
            val scale = appSettingsRepository.scaleUi.collectAsState(null).value
            val linesHeightType by appSettingsRepository.linesHeightTypeState.collectAsState()
            val avatarProviderType by appSettingsRepository.avatarProviderTypeFlow.collectAsState()
            val dateTimeFormat =
                DateTimeFormat(true, null, true, true)//by appSettingsRepository.dateTimeFormatFlow.collectAsState()

            val windowState = rememberWindowState(
                placement = windowPlacement,
                size = DpSize(1280.dp, 720.dp)
            )

            // Save window state for next time the Window is started
            appSettingsRepository.windowPlacement = windowState.placement.preferenceValue

            val currentTab = tabsManager.currentTab.collectAsState().value

            val currentTabName = (currentTab?.tabName?.value ?: NEW_TAB_DEFAULT_NAME).take(MAX_CHARS_CURRENT_TAB_NAME)

            LaunchedEffect(isOpen) {
                if (!isOpen) {
                    tempFilesManager.clearAll()
                    appStateManager.cancelCoroutines()
                    this@application.exitApplication()
                }
            }

            Window(
                title = "${System.getenv("title") ?: AppConstants.APP_NAME} - $currentTabName",
                onCloseRequest = {
                    isOpen = false
                },
                state = windowState,
                icon = painterResource(Res.drawable.logo),
            ) {
                val compositionValues: MutableList<ProvidedValue<*>> =
                    mutableListOf(LocalTextContextMenu provides AppPopupMenu())

                if (scale != null) {
                    compositionValues.add(LocalDensity provides Density(scale, 1f))
                }

                val avatarProvider = when (avatarProviderType) {
                    AvatarProviderType.GRAVATAR -> GravatarAvatarProvider()
                    // TODO this else shouldn't be necessary as avatar provider should not be null and have a
                    //  default value handled by domain layer
                    else -> NoneAvatarProvider()
                }

                compositionValues.add(LocalAvatarProvider provides avatarProvider)
                compositionValues.add(LocalDateTimeFormat provides dateTimeFormat)

                CompositionLocalProvider(
                    values = compositionValues.toTypedArray()
                ) {
                    AppTheme(
                        selectedTheme = theme,
                        customTheme = customTheme,
                        linesHeightType = linesHeightType,
                    ) {
                        Box(modifier = Modifier.background(MaterialTheme.colors.background)) {
                            AppTabs()
                        }
                    }
                }
            }
        }
    }

    private fun initNativeDependencies() {
        val gitnuroRsName = when (currentOs) {
            OS.LINUX -> "libgitnuro_rs.so"
            OS.WINDOWS -> "gitnuro_rs.dll"
            OS.MAC -> "libgitnuro_rs.dylib"
            else -> throw Exception("OS not supported")
        }

        val gitnuroRsInputStream = javaClass.getResourceAsStream("/$gitnuroRsName")

        gitnuroRsInputStream?.use { inputStream ->
            val tempDir = tempFilesManager.tempDir()
            val gitnuroRsFile = File(tempDir, gitnuroRsName)
            val outputStream = FileOutputStream(gitnuroRsFile)

            inputStream.copyTo(outputStream)
            outputStream.flush()
            outputStream.close()

            System.load(gitnuroRsFile.absolutePath)
        } ?: throw Exception("GitnuroRs native dependency not found")
    }

    private fun initProxySettings() {
        // TODO Reenable this in domain layer
        /*appStateManager.appScope.launch {
            appSettingsRepository.proxyFlow.collect { proxySettings ->
                if (proxySettings.useProxy) {
                    when (proxySettings.proxyType) {
                        ProxyType.HTTP -> setHttpProxy(proxySettings)
                        ProxyType.SOCKS -> setSocksProxy(proxySettings)
                    }
                } else {
                    clearProxySettings()
                }
            }
        }*/
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
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .onPreviewKeyEvent {
                        when {
                            it.matchesBinding(KeybindingOption.OPEN_NEW_TAB) -> {
                                tabsManager.addNewEmptyTab()
                                true
                            }

                            it.matchesBinding(KeybindingOption.CLOSE_CURRENT_TAB) -> {
                                tabsManager.closeTab(currentTab)
                                true
                            }

                            it.matchesBinding(KeybindingOption.CHANGE_CURRENT_TAB_LEFT) -> {
                                val tabToSelect = tabs.getOrNull(tabs.indexOf(currentTab) - 1)
                                if (tabToSelect != null) {
                                    tabsManager.selectTab(tabToSelect)
                                }
                                true
                            }

                            it.matchesBinding(KeybindingOption.CHANGE_CURRENT_TAB_RIGHT) -> {
                                val tabToSelect = tabs.getOrNull(tabs.indexOf(currentTab) + 1)
                                if (tabToSelect != null) {
                                    tabsManager.selectTab(tabToSelect)
                                }
                                true
                            }

                            else -> false
                        }
                    }
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

                CompositionLocalProvider(LocalTab provides currentTab) {
                    TabContent(currentTab)
                }
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
                onMoveTab = { fromIndex, toIndex ->
                    tabsManager.onMoveTab(fromIndex, toIndex)
                },
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
            AppTab(currentTab.repositoryTabViewModel)
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
