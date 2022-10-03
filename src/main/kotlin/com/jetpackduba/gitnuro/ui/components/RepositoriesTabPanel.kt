@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.di.DaggerTabComponent
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.theme.primaryTextColor
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
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
                isSelected = tab.key == selectedTabKey,
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
            .fillMaxHeight(),
    ) {
        this.tabs()

        item {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onNewTabClicked,
                    modifier = Modifier
                        .size(36.dp)
                        .pointerHoverIcon(PointerIconDefaults.Hand)
                        .align(Alignment.CenterStart),
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
                            tint = MaterialTheme.colors.primaryTextColor
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