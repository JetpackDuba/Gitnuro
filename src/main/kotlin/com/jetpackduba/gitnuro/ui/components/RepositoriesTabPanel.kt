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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.managers.AppStateManager
import com.jetpackduba.gitnuro.LocalTabScope
import com.jetpackduba.gitnuro.di.AppComponent
import com.jetpackduba.gitnuro.di.DaggerTabComponent
import com.jetpackduba.gitnuro.di.TabComponent
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.viewmodels.TabViewModel
import com.jetpackduba.gitnuro.viewmodels.TabViewModelsHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.name

@Composable
fun RepositoriesTabPanel(
    tabs: List<TabInformation>,
    currentTab: TabInformation?,
    tabsHeight: Dp,
    onTabSelected: (TabInformation) -> Unit,
    onTabClosed: (TabInformation) -> Unit,
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
            Tooltip(
                "\"Shift + Mouse wheel\" to scroll"
            ) {
                HorizontalScrollbar(
                    modifier = Modifier
                        .fillMaxWidth(),
                    adapter = scrollAdapter
                )
            }
        }

        Row {
            LazyRow(
                modifier = Modifier
                    .height(tabsHeight)
                    .weight(1f, false),
                state = stateHorizontal,
            ) {
                items(items = tabs) { tab ->
                    Tooltip(tab.path) {
                        Tab(
                            title = tab.tabName,
                            isSelected = currentTab == tab,
                            isNewTab = tab.path == null,
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
    title: MutableState<String>,
    isSelected: Boolean,
    isNewTab: Boolean,
    onClick: () -> Unit, onCloseTab: () -> Unit
) {
    val backgroundColor = if (isSelected)
        MaterialTheme.colors.surface
    else
        MaterialTheme.colors.background

    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()
    val tabTitle = if (isNewTab)
        title.value
    else
        title.value.replace(
            " ",
            "-"
        ) // Long tab names with spaces make compose not taking full text width for the tab. More info https://issuetracker.google.com/issues/278044455

    Box(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .width(IntrinsicSize.Min)
            .fillMaxHeight()
            .hoverable(hoverInteraction)
            .handMouseClickable { onClick() }
            .background(backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = tabTitle,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
                    .widthIn(max = 720.dp),
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.body2,
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