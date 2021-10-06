package app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import app.theme.primaryLight


@Composable
fun RepositoriesTabPanel(
    modifier: Modifier = Modifier,
    tabs: List<TabInformation>,
    selectedTabKey: Int,
    onTabSelected: (Int) -> Unit,
    onTabsUpdated: (List<TabInformation>) -> Unit,
    newTabContent: @Composable (tabTitle: MutableState<String>) -> Unit,
) {
    var tabsIdentifier by remember { mutableStateOf(0) }

    TabPanel(
        modifier = modifier,
        onNewTabClicked = {
            val tabsCopy = tabs.toMutableList()

            tabsIdentifier++

            val tabName = mutableStateOf("New tab")

            tabsCopy.add(
                TabInformation(tabName, key = tabsIdentifier) {
                    newTabContent(tabName)
                }
            )

            onTabSelected(tabsIdentifier)
            onTabsUpdated(tabsCopy)
        }
    ) {
        tabs.forEach { tab ->
            Tab(
                title = tab.title,
                selected = tab.key == selectedTabKey,
                onClick = {
                    onTabSelected(tab.key)
                },
                onCloseTab = {
                    val isTabSelected = selectedTabKey == tab.key
                    val index = tabs.indexOf(tab)
                    val nextIndex = if (index == 0 && tabs.count() >= 2) {
                        0
                    } else if (tabs.count() >= 2)
                        index - 1
                    else
                        -1

                    val tabsCopy = tabs.toMutableList()
                    tabsCopy.remove(tab)

                    val nextKey = if (nextIndex >= 0)
                        tabsCopy[nextIndex].key
                    else
                        -1

                    if (isTabSelected) {
                        if (nextKey >= 0) {
                            onTabSelected(nextKey)
                        } else {
                            tabsIdentifier++

                            val tabName = mutableStateOf("New tab")
                            tabsCopy.add(
                                TabInformation(tabName, key = tabsIdentifier) {
                                    newTabContent(tabName)
                                }
                            )

                            onTabSelected(tabsIdentifier)
                        }
                    }

                    onTabsUpdated(tabsCopy)
                }
            )
        }
    }
}


@Composable
fun TabPanel(
    modifier: Modifier = Modifier,
    onNewTabClicked: () -> Unit,
    tabs: @Composable RowScope.() -> Unit
) {
    Row(modifier = modifier) {
        this.tabs()

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

@Composable
fun Tab(title: MutableState<String>, selected: Boolean, onClick: () -> Unit, onCloseTab: () -> Unit) {
    Card {
        Box(
            modifier = Modifier
                .padding(horizontal = 1.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (selected) MaterialTheme.colors.primary else primaryLight)
                .clickable { onClick() },
        ) {
            Text(
                text = title.value,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 32.dp),
                color = contentColorFor(MaterialTheme.colors.primary),
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
    val title: MutableState<String>,
    val key: Int,
    val content: @Composable () -> Unit
)