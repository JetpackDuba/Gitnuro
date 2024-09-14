package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.v2.maxScrollOffset
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.di.DaggerTabComponent
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.onMiddleMouseButtonClick
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.ui.components.tooltip.DelayedTooltip
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.drag_sorting.HorizontalDraggableItem
import com.jetpackduba.gitnuro.ui.drag_sorting.horizontalDragContainer
import com.jetpackduba.gitnuro.ui.drag_sorting.rememberHorizontalDragDropState
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RepositoriesTabPanel(
    tabs: List<TabInformation>,
    currentTab: TabInformation?,
    onTabSelected: (TabInformation) -> Unit,
    onTabClosed: (TabInformation) -> Unit,
    onMoveTab: (Int, Int) -> Unit,
    onAddNewTab: () -> Unit,
) {
    val stateHorizontal = rememberLazyListState()
    val scrollAdapter = rememberScrollbarAdapter(stateHorizontal)
    val scope = rememberCoroutineScope()
    var latestTabCount by remember { mutableStateOf(tabs.count()) }

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

    Column {
        if (canBeScrolled) {
            DelayedTooltip(
                "\"Shift + Mouse wheel\" to scroll"
            ) {
                HorizontalScrollbar(
                    modifier = Modifier
                        .fillMaxWidth(),
                    adapter = scrollAdapter
                )
            }
        }


        val dragDropState = rememberHorizontalDragDropState(stateHorizontal) { fromIndex, toIndex ->
            onMoveTab(fromIndex, toIndex)
        }

        Row {
            LazyRow(
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f, false)
                    .horizontalDragContainer(
                        dragDropState = dragDropState,
                        onDraggedItem = {
                            val tab = tabs.getOrNull(it)

                            if (tab != null) {
                                onTabSelected(tab)
                            }
                        },
                    ),
                state = stateHorizontal,
            ) {
                itemsIndexed(
                    items = tabs,
                    key = { _, tab -> tab.tabViewModel }
                ) { index, tab ->
                    HorizontalDraggableItem(dragDropState, index) { _ ->
                        InstantTooltip(tab.path) {
                            Tab(
                                modifier = Modifier,
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

    LaunchedEffect(tabs.count()) {
        // Scroll to the end if a new tab has been added & it's empty (so it's not a new submodule tab)
        if (latestTabCount < tabs.count() && currentTab?.path == null) {
            scope.launch {
                delay(50) // add small delay to wait until [scrollAdapter.maxScrollOffset] is recalculated. Seems more like a hack of some kind...
                scrollAdapter.scrollTo(scrollAdapter.maxScrollOffset)
            }
        }

        latestTabCount = tabs.count()
    }
}

@Composable
fun Tab(
    modifier: Modifier,
    title: MutableState<String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onCloseTab: () -> Unit,
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colors.surface
    else
        MaterialTheme.colors.background

    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .widthIn(min = 200.dp)
            .width(IntrinsicSize.Max)
            .fillMaxHeight()
            .hoverable(hoverInteraction)
            .handMouseClickable { onClick() }
            .onMiddleMouseButtonClick {
                onCloseTab()
            }
            .background(backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title.value,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
                    .widthIn(max = 720.dp),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body2,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                softWrap = false,
            )

            IconButton(
                onClick = onCloseTab,
                enabled = isHovered || isSelected,
                modifier = Modifier
                    .alpha(if (isHovered || isSelected) 1f else 0f)
                    .padding(horizontal = 8.dp)
                    .size(14.dp)
            ) {
                Icon(
                    painterResource(AppIcons.CLOSE),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground
                )
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

    var path = initialPath
        private set

    init {
        tabComponent.inject(this)

        if (initialPath != null) {
            tabName.value = Path(initialPath).name
        }

        tabViewModel.onRepositoryChanged = { newPath ->
            this.path = newPath

            if (newPath == null) {
                tabName.value = DEFAULT_NAME
            } else {
                tabName.value = Path(newPath).name
                appStateManager.repositoryTabChanged(newPath)
            }

            onTabPathChanged()
        }

        // Set the path that should be loaded when the tab is selected for the first time
        tabViewModel.initialPath = initialPath
    }

    fun dispose() {
        tabViewModel.dispose()
    }

    companion object {
        const val DEFAULT_NAME = "New tab"
    }
}

fun emptyTabInformation() = TabInformation(mutableStateOf(""), "", {}, null)
