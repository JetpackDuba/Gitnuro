package app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.compose.ui.zIndex
import app.di.DaggerAppComponent
import app.git.GitManager
import app.theme.AppTheme
import app.ui.AppTab
import app.ui.components.DialogBox
import app.ui.components.RepositoriesTabPanel
import app.ui.components.TabInformation
import javax.inject.Inject
import javax.inject.Provider

class Main {
    val appComponent = DaggerAppComponent.create()

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
                    size = WindowSize(1280.dp, 720.dp)
                )
            ) {
                AppTheme {
                    val showDialog = remember { mutableStateOf(false) }
                    val dialogManager = remember { DialogManager(showDialog) }

                    Box {

                        AppTabs(dialogManager)

                        if (showDialog.value) {
                            val interactionSource = remember { MutableInteractionSource() }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.8f)
                                    .background(Color.Black)
                                    .clickable(
                                        enabled = true,
                                        onClick = {},
                                        interactionSource = interactionSource,
                                        indication = null
                                    )
                            )
                            DialogBox(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .clickable(
                                        enabled = true,
                                        onClick = {},
                                        interactionSource = interactionSource,
                                        indication = null
                                    )
                            ) {
                                dialogManager.dialog()
                            }
                        }
                    }

                }
            }
        }
    }


    @Composable
    fun AppTabs(dialogManager: DialogManager) {
        val tabs = remember {

            val repositoriesSavedTabs = appStateManager.openRepositoriesPathsTabs
            var repoTabs = repositoriesSavedTabs.map { repositoryTab ->
                newAppTab(
                    dialogManager = dialogManager,
                    key = repositoryTab.key,
                    path = repositoryTab.value
                )
            }

            if (repoTabs.isEmpty()) {
                repoTabs = listOf(
                    newAppTab(
                        dialogManager = dialogManager
                    )
                )
            }

            mutableStateOf(repoTabs)
        }

        var selectedTabKey by remember { mutableStateOf(0) }

        Column(
            modifier =
            Modifier.background(MaterialTheme.colors.surface)
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
                    selectedTabKey = selectedTabKey,
                    onTabSelected = { newSelectedTabKey ->
                        selectedTabKey = newSelectedTabKey
                    },
                    newTabContent = { key ->
                        newAppTab(
                            dialogManager = dialogManager,
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

    private fun newAppTab(
        dialogManager: DialogManager,
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

            AppTab(gitManager, dialogManager, path, tabName)
        }
    }
}

class DialogManager(private val showDialog: MutableState<Boolean>) {
    private var content: @Composable () -> Unit = {}

    fun show(content: @Composable () -> Unit) {
        this.content = content
        showDialog.value = true
    }

    fun dismiss() {
        showDialog.value = false
    }

    @Composable
    fun dialog() {
        content()
    }
}

@Composable
fun LoadingRepository() {
    Box { }
}
