@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui.log

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.common.printLog
import com.jetpackduba.gitnuro.domain.BranchesConstants.LOCAL_PREFIX_LENGTH
import com.jetpackduba.gitnuro.domain.extensions.isCherryPicking
import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.domain.extensions.isReverting
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.extensions.backgroundIf
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.toSmartSystemString
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.AvatarImage
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltipPosition
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.ui.resizePointerIconEast
import com.jetpackduba.gitnuro.viewmodels.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.RepositoryState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val colors = listOf(
    Color(0xFF42a5f5),
    Color(0xFFef5350),
    Color(0xFFe78909c),
    Color(0xFFff7043),
    Color(0xFF66bb6a),
    Color(0xFFec407a),
)

private const val CANVAS_MIN_WIDTH = 100
private const val CANVAS_DEFAULT_WIDTH = 120
private const val MIN_GRAPH_LANES = 2

private const val HORIZONTAL_SCROLL_PIXELS_MULTIPLIER = 10

/**
 * Additional number of lanes to simulate to create a margin at the end of the graph.
 */
private const val MARGIN_GRAPH_LANES = 2
private const val LANE_WIDTH = 30f
private const val DIVIDER_WIDTH = 8

private const val LOG_BOTTOM_PADDING = 80

private const val MIN_COMMITS_BEFORE_REQUESTING_MORE = INCREMENTAL_COMMITS_LOAD

private const val TAG = "LogView"

// TODO Min size for message column
@Composable
fun Log(
    viewModel: LogViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    onCreateBranch: (Commit) -> Unit,
    onResetBranch: (Commit) -> Unit,
    onCreateTag: (Commit) -> Unit,
    onChangeUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
) {
    val logStatusState = viewModel.logState.collectAsState()
    val logStatus = logStatusState.value
    var graphPadding by remember(viewModel) { mutableStateOf(viewModel.graphPadding) }

    LaunchedEffect(logStatus.verticalScrollState, logStatus.commitList) {
        launch {
            viewModel.focusCommit.collect { commit ->
                scrollToCommit(logStatus.verticalScrollState, logStatus.commitList, commit.commit)
            }
        }
        launch {
            viewModel.scrollToUncommittedChanges.collect {
                scrollToUncommittedChanges(logStatus.verticalScrollState, logStatus.commitList)
            }
        }
    }

    LogView(
        logState = logStatus,
        selectedItem = selectedItem,
        repositoryState = repositoryState,
        graphPadding = graphPadding,
        onRequestMoreLogItems = { firstVisibleItemIndex -> viewModel.loadMoreLogItems(firstVisibleItemIndex) },
        onCreateBranch = onCreateBranch,
        onResetBranch = onResetBranch,
        onCreateTag = onCreateTag,
        onChangeUpstreamBranch = onChangeUpstreamBranch,
        onRenameBranch = onRenameBranch,
        onGraphPaddingChange = { newGraphPadding ->
            graphPadding = newGraphPadding
            viewModel.graphPadding = newGraphPadding
        },
        onAction = { viewModel.onAction(it) },
        searchView = {
            SearchFilter(
                logViewModel = viewModel,
                searchFilterResults = it,
                searchFocused = { viewModel.addSearchToCloseableView() },
            )
        }
    )
}

@Composable
private fun LogView(
    logState: LogState,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    graphPadding: Float,
    onRequestMoreLogItems: (Int) -> Unit,
    onCreateBranch: (Commit) -> Unit,
    onResetBranch: (Commit) -> Unit,
    onCreateTag: (Commit) -> Unit,
    onChangeUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    onGraphPaddingChange: (Float) -> Unit,
    onAction: (LogAction) -> Unit,
    searchView: @Composable (LogSearch.SearchResults) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val hasUncommittedChanges = logState.hasUncommittedChanges
    val commitList = logState.commitList

    val verticalScrollState = logState.verticalScrollState
    val horizontalScrollState = logState.horizontalScrollState
    val searchFilterValue = logState.searchFilter

    LaunchedEffect(verticalScrollState) {
        snapshotFlow { verticalScrollState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect {
                val commitsList = logState.commitList

                // TODO Check what would happen with a repo with multiple starting commits
                if (
                    commitsList.commits.isNotEmpty() &&
                    commitsList.commits.count() - it < MIN_COMMITS_BEFORE_REQUESTING_MORE &&
                    commitsList.commits.last().commit.parentCount > 0
                ) {
                    printLog(TAG, "Requesting more items")
                    onRequestMoreLogItems(verticalScrollState.firstVisibleItemIndex)
                }
            }
    }

    val selectedCommit = if (selectedItem is SelectedItem.CommitBasedItem) {
        selectedItem.commit
    } else {
        null
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        var graphWidth = (CANVAS_DEFAULT_WIDTH + graphPadding).dp

        if (graphWidth.value < CANVAS_MIN_WIDTH) graphWidth = CANVAS_MIN_WIDTH.dp

        val maxLinePosition = if (commitList.isNotEmpty())
            commitList.maxLane
        else
            MIN_GRAPH_LANES

        var graphRealWidth = ((maxLinePosition + MARGIN_GRAPH_LANES) * LANE_WIDTH).dp

        // Using remember(graphRealWidth, graphWidth) makes the selected background color glitch when changing tabs
        if (graphRealWidth < graphWidth) {
            graphRealWidth = graphWidth
        }


        if (searchFilterValue is LogSearch.SearchResults) {
            searchView(searchFilterValue)
        }

        GraphHeader(
            graphWidth = graphWidth,
            onPaddingChange = {
                onGraphPaddingChange(graphPadding + it)
            },
            onShowSearch = {
                onAction(LogAction.SearchValueChange(""))
            }
        )

        Box {

            // This Box is only used to get a scroll state. With this scroll state we will manually add an offset in
            // the messages list
            Box(
                Modifier
                    .width(graphWidth)
                    .fillMaxHeight()
                    .horizontalScroll(horizontalScrollState)
                    .padding(bottom = 8.dp)
            ) {
                // The content has to be bigger in order to show the scroll bar in the parent component
                Box(
                    modifier = Modifier.width(graphRealWidth)
                )
            }

            CommitsList(
                scrollState = verticalScrollState,
                horizontalScrollState = horizontalScrollState,
                hasUncommittedChanges = hasUncommittedChanges,
                searchFilter = if (searchFilterValue is LogSearch.SearchResults) searchFilterValue.commits else null,
                selectedCommit = selectedCommit,
                logState = logState,
                repositoryState = repositoryState,
                selectedItem = selectedItem,
                commitList = commitList,
                graphWidth = graphWidth,
                onCreateBranch = onCreateBranch,
                onResetBranch = onResetBranch,
                onCreateTag = onCreateTag,
                onChangeUpstreamBranch = onChangeUpstreamBranch,
                onRenameBranch = onRenameBranch,
                onAction = onAction,
            )

            val density = LocalDensity.current.density
            DividerLog(
                modifier = Modifier.draggable(
                    rememberDraggableState {
                        onGraphPaddingChange(graphPadding + it / density)
                    }, Orientation.Horizontal
                ),
                graphWidth = graphWidth,
            )


            // Scrollbar used to scroll horizontally the graph nodes
            // Added after every component to have the highest priority when clicking
            HorizontalScrollbar(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .width(graphWidth)
                    .padding(start = 4.dp, bottom = 4.dp), style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colors.scrollbarNormal,
                    hoverColor = MaterialTheme.colors.scrollbarHover,
                ),
                adapter = rememberScrollbarAdapter(horizontalScrollState)
            )

            val isFirstItemVisible by remember {
                derivedStateOf { verticalScrollState.firstVisibleItemIndex > 0 }
            }

            if (isFirstItemVisible) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(50))
                        .handMouseClickable {
                            scope.launch {
                                verticalScrollState.scrollToItem(0)
                            }
                        }
                        .background(MaterialTheme.colors.primary)
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(Res.drawable.align_top),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.size(20.dp),
                        )

                        Text(
                            text = "Scroll to top",
                            modifier = Modifier.padding(start = 8.dp),
                            color = MaterialTheme.colors.onPrimary,
                            maxLines = 1,
                            style = MaterialTheme.typography.body2,
                        )
                    }
                }
            }
        }
    }
}

suspend fun scrollToCommit(
    verticalScrollState: LazyListState,
    commitList: GraphCommits,
    commit: Commit?,
) {
    val index = commitList.commits.indexOfFirst { it.hash == commit?.hash }
    // TODO Show a message informing the user why we aren't scrolling
    // Index can be -1 if the ref points to a commit that is not shown in the graph due to the limited
    // number of displayed commits.
    if (index >= 0) verticalScrollState.scrollToItem(index)
}

suspend fun scrollToUncommittedChanges(
    verticalScrollState: LazyListState,
    commitList: GraphCommits,
) {
    if (commitList.isNotEmpty())
        verticalScrollState.scrollToItem(0)
}

@Composable
fun SearchFilter(
    logViewModel: LogViewModel,
    searchFilterResults: LogSearch.SearchResults,
    searchFocused: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var searchFilterText by remember { mutableStateOf(logViewModel.savedSearchFilter) }
    val textFieldFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        textFieldFocusRequester.requestFocus()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = searchFilterText,
            onValueChange = {
                searchFilterText = it
                scope.launch {
                    logViewModel.onSearchValueChanged(it)
                }
            },
            maxLines = 1,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(textFieldFocusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        searchFocused()
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    when {
                        keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) -> {
                            scope.launch {
                                logViewModel.selectNextFilterCommit()
                            }
                            true
                        }

                        else -> false
                    }
                },
            label = {
                Text("Search by message, author name or commit ID")
            },
            colors = textFieldColors(),
            textStyle = MaterialTheme.typography.body1,
            trailingIcon = {
                Row(
                    modifier = Modifier
                        .fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (searchFilterText.isNotEmpty()) {
                        Text(
                            "${searchFilterResults.index}/${searchFilterResults.totalCount}",
                            color = MaterialTheme.colors.onBackgroundSecondary,
                        )
                    }

                    IconButton(
                        modifier = Modifier
                            .handOnHover(),
                        onClick = {
                            scope.launch { logViewModel.selectPreviousFilterCommit() }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }

                    IconButton(
                        modifier = Modifier
                            .handOnHover(),
                        onClick = {
                            scope.launch { logViewModel.selectNextFilterCommit() }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }

                    IconButton(
                        modifier = Modifier
                            .handOnHover()
                            .padding(end = 4.dp),
                        onClick = { logViewModel.closeSearch() }
                    ) {
                        Icon(painterResource(Res.drawable.close), contentDescription = null)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommitsList(
    scrollState: LazyListState,
    hasUncommittedChanges: Boolean,
    searchFilter: List<GraphCommit>?,
    selectedCommit: Commit?,
    logState: LogState,
    repositoryState: RepositoryState,
    selectedItem: SelectedItem,
    commitList: GraphCommits,
    onAction: (LogAction) -> Unit,
    onCreateBranch: (Commit) -> Unit,
    onResetBranch: (Commit) -> Unit,
    onCreateTag: (Commit) -> Unit,
    onChangeUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    graphWidth: Dp,
    horizontalScrollState: ScrollState,
) {
    val scope = rememberCoroutineScope()

    ScrollableLazyColumn(
        state = scrollState,
        modifier = Modifier
            .fillMaxSize()
            // The underlying composable assigned to the horizontal scroll bar won't be receiving the scroll events
            // because the commits list will consume the events, so this code tries to scroll manually when it detects
            // horizontal scrolling
            .onPointerEvent(PointerEventType.Scroll) { pointerEvent ->
                scope.launch {
                    val xScroll = pointerEvent.changes.map { it.scrollDelta.x }.sum()
                    horizontalScrollState.scrollBy(xScroll * HORIZONTAL_SCROLL_PIXELS_MULTIPLIER)
                }
            },
    ) {
        if (
            hasUncommittedChanges ||
            repositoryState.isMerging ||
            repositoryState.isRebasing ||
            repositoryState.isCherryPicking
        ) {
            item {
                Box(
                    modifier = Modifier.height(MaterialTheme.linesHeight.logCommitHeight)
                        .clipToBounds()
                        .fillMaxWidth()
                        .handMouseClickable { onAction(LogAction.UncommittedChangesSelected) }
                ) {
                    UncommittedChangesGraphNode(
                        hasPreviousCommits = commitList.commits.isNotEmpty(),
                        isSelected = selectedItem is SelectedItem.UncommittedChanges,
                        modifier = Modifier.offset(-horizontalScrollState.value.dp)
                    )

                    UncommittedChangesLine(
                        graphWidth = graphWidth,
                        isSelected = selectedItem == SelectedItem.UncommittedChanges,
                        statusSummary = logState.statusSummary,
                        repositoryState = repositoryState,
                    )
                }
            }
        }

        // Setting a key makes the graph preserve the scroll position when a new line has been added on top (uncommitted changes)
        // Therefore, after popping a stash, the uncommitted changes wouldn't be visible and requires the user scrolling.
        items(items = commitList) { graphNode ->
            CommitLine(
                graphWidth = graphWidth,
                graphNode = graphNode,
                isSelected = selectedCommit?.hash == graphNode.hash,
                currentBranch = logState.currentBranch,
                matchesSearchFilter = searchFilter?.contains(graphNode),
                horizontalScrollState = horizontalScrollState,
                showCreateNewBranch = { onCreateBranch(graphNode.commit) },
                showCreateNewTag = { onCreateTag(graphNode.commit) },
                resetBranch = { onResetBranch(graphNode.commit) },
                onMergeBranch = { onAction(LogAction.Merge(it)) },
                onDeleteBranch = { onAction(LogAction.DeleteBranch(it)) },
                onDeleteRemoteBranch = { onAction(LogAction.DeleteRemoteBranch(it)) },
                onCheckoutTag = { onAction(LogAction.CheckoutTag(it)) },
                onDeleteTag = { onAction(LogAction.DeleteTag(it)) },
                onPushToRemoteBranch = { onAction(LogAction.PushToRemoteBranch(it)) },
                onPullFromRemoteBranch = { onAction(LogAction.PullFromRemoteBranch(it)) },
                onRebaseBranch = { onAction(LogAction.Rebase(it)) },
                onRebaseInteractive = { onAction(LogAction.RebaseInteractive(graphNode.commit)) },
                onRevCommitSelected = { onAction(LogAction.CommitSelected(graphNode.commit)) },
                onChangeDefaultUpstreamBranch = { onChangeUpstreamBranch(it) },
                onRenameBranch = { onRenameBranch(it) },
                onDeleteStash = { onAction(LogAction.DeleteStash(graphNode.commit)) },
                onApplyStash = { onAction(LogAction.ApplyStash(graphNode.commit)) },
                onPopStash = { onAction(LogAction.PopStash(graphNode.commit)) },
                onCheckoutCommit = { onAction(LogAction.CheckoutCommit(graphNode.commit)) },
                onRevertCommit = { onAction(LogAction.RevertCommit(graphNode.commit)) },
                onCherryPickCommit = { onAction(LogAction.CherryPickCommit(graphNode.commit)) },
                onCheckoutRemoteBranch = { onAction(LogAction.CheckoutRemoteBranch(it)) },
                onCheckoutBranch = { onAction(LogAction.CheckoutBranch(it)) },
                onCopyBranchNameToClipboard = { onAction(LogAction.CopyBranchNameToClipboard(it)) },
            )
        }

        item {
            Box(modifier = Modifier.height(LOG_BOTTOM_PADDING.dp))
        }
    }
}


@Composable
fun GraphHeader(
    graphWidth: Dp,
    onPaddingChange: (Float) -> Unit,
    onShowSearch: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(MaterialTheme.colors.tertiarySurface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .width(graphWidth)
                    .padding(start = 16.dp),
                text = "Graph",
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            val density = LocalDensity.current.density

            SimpleDividerLog(
                modifier = Modifier.draggable(
                    rememberDraggableState {
                        onPaddingChange(it / density) // Divide by density for screens with scaling > 1
                    }, Orientation.Horizontal
                ),
            )

            Text(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                text = "Message",
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            IconButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .handOnHover(),
                onClick = onShowSearch
            ) {
                Icon(
                    painterResource(Res.drawable.search),
                    modifier = Modifier.size(18.dp),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

@Composable
fun UncommittedChangesLine(
    graphWidth: Dp,
    isSelected: Boolean,
    repositoryState: RepositoryState,
    statusSummary: StatusSummary,
) {
    Row(
        modifier = Modifier
            .height(MaterialTheme.linesHeight.logCommitHeight)
            .padding(start = graphWidth)
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            .padding(DIVIDER_WIDTH.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = when {
            repositoryState.isRebasing -> "Pending changes to rebase"
            repositoryState.isMerging -> "Pending changes to merge"
            repositoryState.isCherryPicking -> "Pending changes to cherry-pick"
            repositoryState.isReverting -> "Pending changes to revert"
            else -> "Uncommitted changes"
        }

        Text(
            text = text,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            color = MaterialTheme.colors.onBackground,
        )

        Spacer(modifier = Modifier.weight(1f))

        LogStatusSummary(
            statusSummary = statusSummary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
fun LogStatusSummary(statusSummary: StatusSummary, modifier: Modifier) {
    Row(
        modifier = modifier,
    ) {
        if (statusSummary.modifiedCount > 0) {
            SummaryEntry(
                count = statusSummary.modifiedCount,
                icon = Icons.Default.Edit,
                color = MaterialTheme.colors.modifyFile,
            )
        }

        if (statusSummary.addedCount > 0) {
            SummaryEntry(
                count = statusSummary.addedCount,
                icon = Icons.Default.Add,
                color = MaterialTheme.colors.addFile,
            )
        }

        if (statusSummary.deletedCount > 0) {
            SummaryEntry(
                count = statusSummary.deletedCount,
                icon = Icons.Default.Delete,
                color = MaterialTheme.colors.deleteFile,
            )
        }

        if (statusSummary.conflictingCount > 0) {
            SummaryEntry(
                count = statusSummary.conflictingCount,
                icon = Icons.Default.Warning,
                color = MaterialTheme.colors.conflictFile,
            )
        }
    }
}

@Composable
fun SummaryEntry(
    count: Int, icon: ImageVector, color: Color,
) {
    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
        )

        Icon(
            imageVector = icon, tint = color, contentDescription = null, modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun CommitLine(
    graphWidth: Dp,
    graphNode: GraphCommit,
    isSelected: Boolean,
    currentBranch: Branch?,
    matchesSearchFilter: Boolean?,
    showCreateNewBranch: () -> Unit,
    showCreateNewTag: () -> Unit,
    resetBranch: () -> Unit,
    onApplyStash: () -> Unit,
    onPopStash: () -> Unit,
    onDeleteStash: () -> Unit,
    onMergeBranch: (Branch) -> Unit,
    onDeleteBranch: (Branch) -> Unit,
    onDeleteRemoteBranch: (Branch) -> Unit,
    onCheckoutTag: (Tag) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onPushToRemoteBranch: (Branch) -> Unit,
    onPullFromRemoteBranch: (Branch) -> Unit,
    onRebaseBranch: (Branch) -> Unit,
    onRevCommitSelected: () -> Unit,
    onRebaseInteractive: () -> Unit,
    onCheckoutCommit: () -> Unit,
    onRevertCommit: () -> Unit,
    onCherryPickCommit: () -> Unit,
    onCheckoutRemoteBranch: (Branch) -> Unit,
    onCheckoutBranch: (Branch) -> Unit,
    onChangeDefaultUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
    onCopyBranchNameToClipboard: (Branch) -> Unit,
    horizontalScrollState: ScrollState,
) {
    val isLastCommitOfCurrentBranch = currentBranch?.hash == graphNode.hash

    ContextMenu(
        items = {
            if (graphNode.isStash) {
                stashesContextMenuItems(
                    onApply = onApplyStash,
                    onPop = onPopStash,
                    onDelete = onDeleteStash,
                )
            } else {
                logContextMenu(
                    onCheckoutCommit = onCheckoutCommit,//{ logViewModel.checkoutCommit(graphNode) },
                    onCreateNewBranch = showCreateNewBranch,
                    onCreateNewTag = showCreateNewTag,
                    onRevertCommit = onRevertCommit,//{ logViewModel.revertCommit(graphNode) },
                    onCherryPickCommit = onCherryPickCommit, //{ logViewModel.cherryPickCommit(graphNode) },
                    onRebaseInteractive = onRebaseInteractive,
                    onResetBranch = { resetBranch() },
                    isLastCommit = isLastCommitOfCurrentBranch
                )
            }
        },
    ) {
        Box(
            modifier = Modifier
                .height(MaterialTheme.linesHeight.logCommitHeight)
                .handMouseClickable { onRevCommitSelected() }
        ) {
            val nodeColor = colors[graphNode.lane % colors.size]

            Box {
                Row(
                    modifier = Modifier
                        .clipToBounds()
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .offset(-horizontalScrollState.value.dp)
                ) {
                    CommitsGraph(
                        modifier = Modifier
                            .fillMaxHeight(),
                        plotCommit = graphNode,
                        nodeColor = nodeColor,
                        isSelected = isSelected,
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = graphWidth)
                    .fillMaxHeight()
                    .background(MaterialTheme.colors.background)
                    .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            ) {

                if (matchesSearchFilter == true) {
                    Box(
                        modifier = Modifier
                            .padding(start = DIVIDER_WIDTH.dp)
                            .background(MaterialTheme.colors.secondary)
                            .fillMaxHeight()
                            .width(4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                ) {
                    CommitMessage(
                        graphCommit = graphNode,
                        nodeColor = nodeColor,
                        matchesSearchFilter = matchesSearchFilter,
                        currentBranch = currentBranch,
                        onCheckoutBranch = { ref ->
                            if (ref.isRemote) {
                                onCheckoutRemoteBranch(ref)
                            } else {
                                onCheckoutBranch(ref)
                            }
                        },
                        onMergeBranch = onMergeBranch,
                        onDeleteBranch = onDeleteBranch,
                        onDeleteRemoteBranch = onDeleteRemoteBranch,
                        onCheckoutTag = onCheckoutTag,
                        onDeleteTag = onDeleteTag,
                        onRebaseBranch = onRebaseBranch,
                        onPushRemoteBranch = onPushToRemoteBranch,
                        onPullRemoteBranch = onPullFromRemoteBranch,
                        onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
                        onRenameBranch = onRenameBranch,
                        onCopyBranchNameToClipboard = onCopyBranchNameToClipboard,
                    )
                }
            }
        }
    }
}

@Composable
fun CommitMessage(
    graphCommit: GraphCommit,
    currentBranch: Branch?,
    nodeColor: Color,
    matchesSearchFilter: Boolean?,
    onCheckoutBranch: (ref: Branch) -> Unit,
    onMergeBranch: (ref: Branch) -> Unit,
    onDeleteBranch: (ref: Branch) -> Unit,
    onDeleteRemoteBranch: (ref: Branch) -> Unit,
    onRebaseBranch: (ref: Branch) -> Unit,
    onCheckoutTag: (tag: Tag) -> Unit,
    onDeleteTag: (tag: Tag) -> Unit,
    onPushRemoteBranch: (ref: Branch) -> Unit,
    onPullRemoteBranch: (ref: Branch) -> Unit,
    onChangeDefaultUpstreamBranch: (ref: Branch) -> Unit,
    onRenameBranch: (ref: Branch) -> Unit,
    onCopyBranchNameToClipboard: (ref: Branch) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize()
            .hoverable(
                // This modifier is added just to prevent committer tooltip is shown then it is underneath this message
                remember { MutableInteractionSource() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            if (!graphCommit.isStash) {
                // TODO Enable this once commits list is migrated to new structure
                for (tag in graphCommit.tags) {
                    TagChip(
                        tag = tag,
                        color = nodeColor,
                        onCheckoutTag = { onCheckoutTag(tag) },
                        onDeleteTag = { onDeleteTag(tag) },
                    )
                }
                for (branch in graphCommit.branches) {
                    BranchChip(
                        ref = branch,
                        color = nodeColor,
                        currentBranch = currentBranch,
                        isCurrentBranch = branch.isSameBranch(currentBranch),
                        onCheckoutBranch = { onCheckoutBranch(branch) },
                        onMergeBranch = { onMergeBranch(branch) },
                        onDeleteBranch = { onDeleteBranch(branch) },
                        onDeleteRemoteBranch = { onDeleteRemoteBranch(branch) },
                        onRebaseBranch = { onRebaseBranch(branch) },
                        onPullRemoteBranch = { onPullRemoteBranch(branch) },
                        onPushRemoteBranch = { onPushRemoteBranch(branch) },
                        onChangeDefaultUpstreamBranch = { onChangeDefaultUpstreamBranch(branch) },
                        onRenameBranch = { onRenameBranch(branch) },
                        onCopyBranchNameToClipboard = { onCopyBranchNameToClipboard(branch) },
                    )
                }
                /*commit.refs.sortedWith { ref1, ref2 ->
                    if (ref1.isSameBranch(currentBranch)) {
                        -1
                    } else {
                        ref1.name.compareTo(ref2.name)
                    }
                }.forEach { ref ->
                    if (ref.isTag) {
                        TagChip(
                            ref = ref,
                            color = nodeColor,
                            onCheckoutTag = { onCheckoutRef(ref) },
                            onDeleteTag = { onDeleteTag(ref) },
                        )
                    } else if (ref.isBranch) {
                        BranchChip(
                            ref = ref,
                            color = nodeColor,
                            currentBranch = currentBranch,
                            isCurrentBranch = ref.isSameBranch(currentBranch),
                            onCheckoutBranch = { onCheckoutRef(ref) },
                            onMergeBranch = { onMergeBranch(ref) },
                            onDeleteBranch = { onDeleteBranch(ref) },
                            onDeleteRemoteBranch = { onDeleteRemoteBranch(ref) },
                            onRebaseBranch = { onRebaseBranch(ref) },
                            onPullRemoteBranch = { onPullRemoteBranch(ref) },
                            onPushRemoteBranch = { onPushRemoteBranch(ref) },
                            onChangeDefaultUpstreamBranch = { onChangeDefaultUpstreamBranch(ref) },
                            onRenameBranch = { onRenameBranch(ref) },
                            onCopyBranchNameToClipboard = { onCopyBranchNameToClipboard(ref) },
                        )
                    }
                }*/
            }
        }

        val message = remember(graphCommit.hash) {
            graphCommit.commit.shortMessage
        }

        Text(
            text = message,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.body2,
            color = if (matchesSearchFilter == false) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        InstantTooltip(
            text = graphCommit.date.toSmartSystemString(allowRelative = false, showTime = true),
            modifier = Modifier.padding(horizontal = 16.dp),
            position = InstantTooltipPosition.RIGHT,
        ) {
            Text(
                text = graphCommit.date.toSmartSystemString(),
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onBackgroundSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DividerLog(modifier: Modifier, graphWidth: Dp) {
    Box(
        modifier = Modifier
            .padding(start = graphWidth)
            .width(DIVIDER_WIDTH.dp)
            .then(modifier)
            .pointerHoverIcon(resizePointerIconEast)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(color = MaterialTheme.colors.primaryVariant)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun SimpleDividerLog(modifier: Modifier) {
    DividerLog(modifier, graphWidth = 0.dp)
}


@Composable
fun CommitsGraph(
    modifier: Modifier = Modifier,
    plotCommit: GraphCommit,
    nodeColor: Color,
    isSelected: Boolean,
) {
    val passingLanes = plotCommit.passingLanes
    val forkingOffLanes = plotCommit.forkingOffLanes
    val mergingLanes = plotCommit.mergingLanes
    val density = LocalDensity.current.density
    val laneWidthWithDensity = remember(density) {
        LANE_WIDTH * density
    }

    Box(
        modifier = modifier
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            .fillMaxHeight(),
        contentAlignment = Alignment.CenterStart,
    ) {

        val itemPosition = plotCommit.lane

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {
                if (plotCommit.childCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), 0f),
                        strokeWidth = 2f * density,
                    )
                }

                forkingOffLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (plotLane + 1), 0f),
                        strokeWidth = 2f * density,
                    )
                }

                mergingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane + 1), this.size.height),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        strokeWidth = 2f * density,
                    )
                }

                if (plotCommit.commit.parentCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.size.height),
                        strokeWidth = 2f * density,
                    )
                }

                passingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane + 1), 0f),
                        end = Offset(laneWidthWithDensity * (plotLane + 1), this.size.height),
                        strokeWidth = 2f * density,
                    )
                }
            }
        }

        CommitNode(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = ((itemPosition + 1) * 30 - 15).dp),
            plotCommit = plotCommit,
            color = nodeColor,
        )
    }
}

@Composable
fun CommitNode(
    modifier: Modifier = Modifier,
    plotCommit: GraphCommit,
    color: Color,
) {
    val author = plotCommit.author
    if (plotCommit.isStash) {
        Box(
            modifier = modifier
                .size(30.dp)
                .border(2.dp, color, shape = CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(Res.drawable.stash),
                modifier = Modifier.size(20.dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
            )
        }
    } else {
        InstantTooltip(
            "${author.name} <${author.email}>",
            position = InstantTooltipPosition.RIGHT,
        ) {
            Box(
                modifier = modifier
                    .size(30.dp)
                    .border(2.dp, color, shape = CircleShape)
                    .clip(CircleShape)
            ) {
                AvatarImage(
                    modifier = Modifier.fillMaxSize(),
                    personIdent = plotCommit.author,
                    color = color,
                )
            }
        }
    }
}

@Composable
fun UncommittedChangesGraphNode(
    modifier: Modifier = Modifier,
    hasPreviousCommits: Boolean,
    isSelected: Boolean,
) {
    val density = LocalDensity.current.density

    val laneWidthWithDensity = remember(density) {
        LANE_WIDTH * density
    }
    Box(
        modifier = modifier
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {
                if (hasPreviousCommits) drawLine(
                    color = colors[0],
                    start = Offset(laneWidthWithDensity, this.center.y),
                    end = Offset(laneWidthWithDensity, this.size.height),
                    strokeWidth = 2f * density,
                )

                drawCircle(
                    color = colors[0],
                    radius = 15f * density,
                    center = Offset(laneWidthWithDensity, this.center.y),
                )
            }
        }
    }
}

@Composable
fun BranchChip(
    modifier: Modifier = Modifier,
    isCurrentBranch: Boolean = false,
    ref: Branch,
    currentBranch: Branch?,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onDeleteRemoteBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onPushRemoteBranch: () -> Unit,
    onPullRemoteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit,
    onRenameBranch: () -> Unit,
    color: Color,
) {
    val contextMenuItemsList = {
        branchContextMenuItems(
            branch = ref,
            currentBranch = currentBranch,
            isCurrentBranch = isCurrentBranch,
            isLocal = ref.isLocal,
            onCheckoutBranch = onCheckoutBranch,
            onMergeBranch = onMergeBranch,
            onDeleteBranch = onDeleteBranch,
            onDeleteRemoteBranch = onDeleteRemoteBranch,
            onRebaseBranch = onRebaseBranch,
            onPushToRemoteBranch = onPushRemoteBranch,
            onPullFromRemoteBranch = onPullRemoteBranch,
            onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
            onRenameBranch = onRenameBranch,
            onCopyBranchNameToClipboard = onCopyBranchNameToClipboard,
        )
    }

    var endingContent: @Composable () -> Unit = {}
    if (isCurrentBranch) {
        endingContent = {
            Icon(
                painter = painterResource(Res.drawable.location),
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
                tint = MaterialTheme.colors.primaryVariant,
            )
        }
    }

    Chip(
        modifier = modifier.draggable(
            rememberDraggableState {

            },
            orientation = Orientation.Vertical,
        ),
        color = color,
        text = ref.logName,
        icon = Res.drawable.branch,
        onCheckoutRef = onCheckoutBranch,
        contextMenuItemsList = contextMenuItemsList,
        endingContent = endingContent,
    )
}

val Branch.logName: String
    get() = when {
        this.name == Constants.HEAD -> {
            this.name
        }

        this.isRemote -> {
            name.replace("refs/remotes/", "")
        }

        else -> {
            val split = this.name.split("/")
            split.takeLast(split.size - LOCAL_PREFIX_LENGTH).joinToString("/")
        }
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    tag: Tag,
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
    color: Color,
) {
    val contextMenuItemsList = {
        tagContextMenuItems(
            onCheckoutTag = onCheckoutTag,
            onDeleteTag = onDeleteTag,
        )
    }

    Chip(
        modifier,
        tag.simpleName,
        Res.drawable.tag,
        onCheckoutRef = onCheckoutTag,
        contextMenuItemsList = contextMenuItemsList,
        color = color,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Chip(
    modifier: Modifier = Modifier,
    text: String,
    icon: DrawableResource,
    color: Color,
    onCheckoutRef: () -> Unit,
    contextMenuItemsList: () -> List<ContextMenuElement>,
    endingContent: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = color, shape = RoundedCornerShape(16.dp))
            .combinedClickable(onDoubleClick = onCheckoutRef, onClick = {})
            .handOnHover()
    ) {
        ContextMenu(
            items = contextMenuItemsList
        ) {
            Row(
                modifier = modifier,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.background(color = color)) {
                    Icon(
                        modifier = Modifier
                            .padding(6.dp)
                            .size(14.dp),
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colors.background,
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                endingContent()
            }
        }
    }
}