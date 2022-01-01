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
import app.theme.tabColorActive
import app.theme.tabColorInactive


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
                title = tab.title,
                selected = tab.key == selectedTabKey,
                onClick = {
                    onTabSelected(tab.key)
                },
                onCloseTab = {
                    val isTabSelected = selectedTabKey == tab.key
                    val index = tabs.indexOf(tab)
                    val nextIndex = if (index == 0 && tabs.count() >= 2) {
                        1
                    } else if (index == tabs.count() -1 && tabs.count() >= 2)
                        index - 1
                    else if (tabs.count() >= 2)
                        index
                    else
                        -1

                    val nextKey = if (nextIndex >= 0)
                        tabs[nextIndex].key
                    else
                        -1

                    if (isTabSelected) {
                        if (nextKey >= 0) {
                            onTabSelected(nextKey)
                        } else {
                            tabsIdentifier++

                            onTabSelected(tabsIdentifier)
                            newTabContent(tabsIdentifier)
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
    val title: MutableState<String>,
    val key: Int,
    val content: @Composable (TabInformation) -> Unit
)