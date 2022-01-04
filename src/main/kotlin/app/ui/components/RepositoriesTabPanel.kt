package app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.AppStateManager
import app.di.AppComponent
import app.di.DaggerTabComponent
import app.viewmodels.TabViewModel
import app.theme.tabColorActive
import app.theme.tabColorInactive
import app.ui.AppTab
import javax.inject.Inject


@Composable
fun RepositoriesTabPanel(
    modifier: Modifier = Modifier,
    tabs: List<TabInformation>,
    selectedTabKey: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    newTabContent: (key: Int) -> TabInformation,
) {
    var tabsIdentifier by remember {
        mutableStateOf(tabs.count())
    }

    TabPanel(
        modifier = modifier,
        onNewTabClicked = {
            tabsIdentifier++

            newTabContent(tabsIdentifier)
            onTabSelected(tabsIdentifier)
        }
    ) {
        items(items = tabs) { tab ->
            Tab(
                title = tab.tabName,
                selected = tab.key == selectedTabKey,
                onClick = {
                    onTabSelected(tab.key)
                },
                onCloseTab = {
                    val isTabSelected = selectedTabKey == tab.key
                    val index = tabs.indexOf(tab)
                    val nextIndex = if (index == 0 && tabs.count() >= 2) {
                        1 // If the first tab is selected, select the next one
                    } else if (index == tabs.count() -1 && tabs.count() >= 2)
                        index - 1 // If the last tab is selected, select the previous one
                    else if (tabs.count() >= 2)
                        index // If any in between tab is selected, select the next one
                    else
                        -1 // If there aren't any additional tabs once we remove this one

                    val nextKey = if (nextIndex >= 0)
                        tabs[nextIndex].key
                    else
                        -1

                    if (isTabSelected) {
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
}


@Composable
fun TabPanel(
    modifier: Modifier = Modifier,
    onNewTabClicked: () -> Unit,
    tabs: LazyListScope.() -> Unit
) {
    LazyRow(
        modifier = modifier,
    ) {
        this.tabs()

        item {
            IconButton(
                onClick = onNewTabClicked,
                modifier = Modifier
                    .size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }
        }
    }
}

@Composable
fun Tab(title: MutableState<String>, selected: Boolean, onClick: () -> Unit, onCloseTab: () -> Unit) {
    Card {
        val backgroundColor = if (selected)
            MaterialTheme.colors.tabColorActive
        else
            MaterialTheme.colors.tabColorInactive

        Box(
            modifier = Modifier
                .padding(horizontal = 1.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(backgroundColor)
                .clickable { onClick() },
        ) {
            Text(
                text = title.value,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 32.dp),
                color = contentColorFor(backgroundColor),
            )
            IconButton(
                onClick = onCloseTab,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(horizontal = 8.dp)
                    .size(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
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
    @Inject
    lateinit var gitManager: TabViewModel

    @Inject
    lateinit var appStateManager: AppStateManager

    val content: @Composable (TabInformation) -> Unit

    init {
        val tabComponent = DaggerTabComponent.builder()
            .appComponent(appComponent)
            .build()
        tabComponent.inject(this)

        //TODO: This shouldn't be here, should be in the parent method
        gitManager.onRepositoryChanged = { path ->
            if (path == null) {
                appStateManager.repositoryTabRemoved(key)
            } else
                appStateManager.repositoryTabChanged(key, path)
        }
        content = {
            AppTab(gitManager, path, tabName)
        }
    }
}