@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.AppStateManager
import app.di.AppComponent
import app.di.DaggerTabComponent
import app.extensions.handMouseClickable
import app.theme.primaryTextColor
import app.viewmodels.TabViewModel
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name

@Composable
fun RepositoriesTabPanel(
    modifier: Modifier = Modifier,
    tabs: List<TabInformation>,
    selectedTabKey: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit,
    newTabContent: (key: Int) -> TabInformation,
) {
    var tabsIdentifier by remember { mutableStateOf(tabs.count()) }

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
                    } else if (index == tabs.count() - 1 && tabs.count() >= 2)
                        index - 1 // If the last tab is selected, select the previous one
                    else if (tabs.count() >= 2)
                        index + 1 // If any in between tab is selected, select the next one
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
        modifier = modifier
            .height(36.dp),
    ) {
        this.tabs()

        item {
            IconButton(
                onClick = onNewTabClicked,
                modifier = Modifier
                    .size(36.dp)
                    .pointerHoverIcon(PointerIconDefaults.Hand),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primaryVariant,
                )
            }
        }
    }
}

@Composable
fun Tab(title: MutableState<String>, selected: Boolean, onClick: () -> Unit, onCloseTab: () -> Unit) {
    val elevation = if (selected) {
        3.dp
    } else
        0.dp
    Box {
        val backgroundColor = if (selected)
            MaterialTheme.colors.surface
        else
            MaterialTheme.colors.background

        Row(
            modifier = Modifier
                .width(180.dp)
                .fillMaxHeight()
                .shadow(elevation = elevation)
                .padding(start = 2.dp, end = 2.dp, top = 2.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .handMouseClickable { onClick() }
                .background(backgroundColor),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title.value,
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .weight(1f),
                color = MaterialTheme.colors.primaryTextColor,
                overflow = TextOverflow.Visible,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
            )
            IconButton(
                onClick = onCloseTab,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(14.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colors.primaryTextColor)
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
    lateinit var tabViewModel: TabViewModel

    @Inject
    lateinit var appStateManager: AppStateManager

    init {
        val tabComponent = DaggerTabComponent.builder()
            .appComponent(appComponent)
            .build()
        tabComponent.inject(this)

        //TODO: This shouldn't be here, should be in the parent method
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