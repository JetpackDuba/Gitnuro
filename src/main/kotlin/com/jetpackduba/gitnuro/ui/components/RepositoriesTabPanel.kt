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
import com.jetpackduba.gitnuro.AppStateManager
import com.jetpackduba.gitnuro.LocalTabScope
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.di.DaggerTabComponent
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import com.jetpackduba.gitnuro.viewmodels.TabViewModelsHolder
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name

@Composable
fun RepositoriesTabPanel(
    tabs: List<TabInformation>,
    currentTab: TabInformation?,
    onTabSelected: (TabInformation) -> Unit,
    onTabClosed: (TabInformation) -> Unit,
    onAddNewTab: () -> Unit,
) {
    val stateHorizontal = rememberLazyListState()

//    LaunchedEffect(selectedTabKey) {
//        val index = tabs.indexOfFirst { it.key == selectedTabKey }
//        // todo sometimes it scrolls to (index - 1) for some weird reason
//        if (index > -1) {
//            stateHorizontal.scrollToItem(index)
//        }
//    }

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
                items(items = tabs) { tab ->
                    Tab(
                        title = tab.tabName,
                        isSelected = currentTab == tab,
                        onClick = {
                            onTabSelected(tab)
                        },
                        onCloseTab = {
                            onTabClosed(tab)
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
                onAddNewTab()
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
    val initialPath: String?,
    val onTabPathChanged: () -> Unit,
    appComponent: AppComponent?,
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

    var path = initialPath
        private set

    init {
        tabComponent.inject(this)

        tabViewModel.onRepositoryChanged = { path ->
            this.path = path

            tabName.value = Path(path).name
            appStateManager.repositoryTabChanged(path)
            onTabPathChanged()
        }
        if (initialPath != null)
            tabViewModel.openRepository(initialPath)
    }

    fun dispose() {
        tabViewModel.dispose()
    }
}

fun emptyTabInformation() = TabInformation(mutableStateOf(""), "", {}, null)

@Composable
inline fun <reified T> gitnuroViewModel(): T {
    val tab = LocalTabScope.current

    return remember(tab) {
        tab.tabViewModelsHolder.viewModels[T::class] as T
    }
}