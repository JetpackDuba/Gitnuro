package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.App
import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.LocalTabScope
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.di.DaggerTabComponent
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.viewmodels.SettingsViewModel
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import com.jetpackduba.gitnuro.viewmodels.TabViewModelsHolder
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name

@Composable
fun RepositoriesTabPanel(
    tabs: List<TabInformation>,
    selectedTabKey: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    newTabContent: (key: Int) -> TabInformation,
) {
    var tabsIdentifier by remember { mutableStateOf(tabs.count()) }
    val stateHorizontal = rememberLazyListState()

    LaunchedEffect(selectedTabKey) {
        val index = tabs.indexOfFirst { it.key == selectedTabKey }
        // todo sometimes it scrolls to (index - 1) for some weird reason
        if (index > -1) {
            stateHorizontal.scrollToItem(index)
        }
    }

    val canBeScrolled by remember {
        derivedStateOf {
            val layoutInfo = stateHorizontal.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val firstVisibleItem = visibleItemsInfo.first()
                val lastVisibleItem = visibleItemsInfo.last()

                val viewportHeight = layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset

                !(firstVisibleItem.index == 0 &&
                        firstVisibleItem.offset == 0 &&
                        lastVisibleItem.index + 1 == layoutInfo.totalItemsCount &&
                        lastVisibleItem.offset + lastVisibleItem.size <= viewportHeight)
            }
        }
    }


    Row {
        Box(
            modifier = Modifier
                .weight(1f, false)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxHeight(),
                state = stateHorizontal,
            ) {
                items(items = tabs, key = { it.key }) { tab ->
                    Tab(
                        title = tab.tabName,
                        isSelected = tab.key == selectedTabKey,
                        onClick = {
                            onTabSelected(tab.key)
                        },
                        onCloseTab = {
                            val isTabSelected = selectedTabKey == tab.key

                            if (isTabSelected) {
                                val nextKey = getTabNextKey(tab, tabs)

                                if (nextKey >= 0) {
                                    onTabSelected(nextKey)
                                } else {
                                    tabsIdentifier++

                                    // Create a new tab if the tabs list is empty after removing the current one
                                    newTabContent(tabsIdentifier)
                                    onTabSelected(tabsIdentifier)
                                }
                            }

                            onTabClosed(tab.key)
                        }
                    )
                }
            }

            if (canBeScrolled) {
                Tooltip(
                    "\"Shift + Mouse wheel\" to scroll"
                ) {
                    HorizontalScrollbar(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .width((tabs.count() * 180).dp),
                        adapter = rememberScrollbarAdapter(stateHorizontal)
                    )
                }
            }
        }

        IconButton(
            onClick = {
                tabsIdentifier++

                newTabContent(tabsIdentifier)
                onTabSelected(tabsIdentifier)
            },
            modifier = Modifier
                .size(36.dp)
                .handOnHover()
                .align(Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = MaterialTheme.colors.primaryVariant,
            )
        }
    }
}


private fun getTabNextKey(tab: TabInformation, tabs: List<TabInformation>): Int {
    val index = tabs.indexOf(tab)
    val nextIndex = if (index == 0 && tabs.count() >= 2) {
        1 // If the first tab is selected, select the next one
    } else if (index == tabs.count() - 1 && tabs.count() >= 2)
        index - 1 // If the last tab is selected, select the previous one
    else if (tabs.count() >= 2)
        index + 1 // If any in between tab is selected, select the next one
    else
        -1 // If there aren't any additional tabs once we remove this one

    return if (nextIndex >= 0)
        tabs[nextIndex].key
    else
        -1
}

@Composable
fun Tab(title: MutableState<String>, isSelected: Boolean, onClick: () -> Unit, onCloseTab: () -> Unit) {
    Box {
        val backgroundColor = if (isSelected)
            MaterialTheme.colors.surface
        else
            MaterialTheme.colors.background

        val hoverInteraction = remember { MutableInteractionSource() }
        val isHovered by hoverInteraction.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .hoverable(hoverInteraction)
                .handMouseClickable { onClick() }
                .background(backgroundColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title.value,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 8.dp)
                        .weight(1f),
                    overflow = TextOverflow.Visible,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                )

                if (isHovered || isSelected) {
                    IconButton(
                        onClick = onCloseTab,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onBackground
                        )
                    }
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colors.primaryVariant)
                )
            }
        }
    }
}

class TabInformation(
    val tabName: MutableState<String>,
    val key: Int,
    val path: String?,
    appComponent: AppComponent,
) {
    private val tabComponent: TabComponent = DaggerTabComponent.builder()
        .appComponent(appComponent)
        .build()

    @Inject
    lateinit var tabViewModel: TabViewModel

    @Inject
    lateinit var appStateManager: AppStateManager

    @Inject
    lateinit var tabViewModelsHolder: TabViewModelsHolder

    init {
        tabComponent.inject(this)

        tabViewModel.onRepositoryChanged = { path ->
            if (path == null) {
                appStateManager.repositoryTabRemoved(key)
            } else {
                tabName.value = Path(path).name
                appStateManager.repositoryTabChanged(key, path)
            }
        }
        if (path != null)
            tabViewModel.openRepository(path)
    }
}

fun emptyTabInformation() = TabInformation(mutableStateOf(""), 0, "", object : AppComponent {
    override fun inject(main: App) {}

    override fun appStateManager(): AppStateManager {
        TODO()
    }

    override fun settingsViewModel(): SettingsViewModel {
        TODO()
    }

    override fun credentialsStateManager(): CredentialsStateManager {
        TODO()
    }

    override fun appPreferences(): AppSettings {
        TODO()
    }
})

@Composable
inline fun <reified T> gitnuroViewModel(): T {
    val tab = LocalTabScope.current

    return remember(tab) {
        tab.tabViewModelsHolder.viewModels[T::class] as T
    }
}