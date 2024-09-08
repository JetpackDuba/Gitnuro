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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.graph.GraphCommitList
import com.jetpackduba.gitnuro.git.graph.GraphNode
import com.jetpackduba.gitnuro.git.workspace.StatusSummary
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.components.AvatarImage
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltip
import com.jetpackduba.gitnuro.ui.components.tooltip.InstantTooltipPosition
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.ui.dialogs.NewBranchDialog
import com.jetpackduba.gitnuro.ui.dialogs.NewTagDialog
import com.jetpackduba.gitnuro.ui.dialogs.ResetBranchDialog
import com.jetpackduba.gitnuro.ui.dialogs.SetDefaultUpstreamBranchDialog
import com.jetpackduba.gitnuro.ui.resizePointerIconEast
import com.jetpackduba.gitnuro.viewmodels.ChangeDefaultUpstreamBranchViewModel
import com.jetpackduba.gitnuro.viewmodels.LogSearch
import com.jetpackduba.gitnuro.viewmodels.LogStatus
import com.jetpackduba.gitnuro.viewmodels.LogViewModel
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit

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

// TODO Min size for message column
@OptIn(
    ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class
)
@Composable
fun Log(
    logViewModel: LogViewModel,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    changeDefaultUpstreamBranchViewModel: () -> ChangeDefaultUpstreamBranchViewModel,
) {
    val logStatusState = logViewModel.logStatus.collectAsState()
    val logStatus = logStatusState.value
    val showLogDialog by logViewModel.logDialog.collectAsState()

    when (logStatus) {
        is LogStatus.Loaded -> LogLoaded(
            logViewModel = logViewModel,
            logStatus = logStatus,
            showLogDialog = showLogDialog,
            selectedItem = selectedItem,
            repositoryState = repositoryState,
            changeDefaultUpstreamBranchViewModel = changeDefaultUpstreamBranchViewModel,
        )

        LogStatus.Loading -> LogLoading()
    }
}

@Composable
private fun LogLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading commits history...",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LogLoaded(
    logViewModel: LogViewModel,
    logStatus: LogStatus.Loaded,
    showLogDialog: LogDialog,
    selectedItem: SelectedItem,
    repositoryState: RepositoryState,
    changeDefaultUpstreamBranchViewModel: () -> ChangeDefaultUpstreamBranchViewModel,
) {
    val scope = rememberCoroutineScope()
    val hasUncommittedChanges = logStatus.hasUncommittedChanges
    val commitList = logStatus.plotCommitList
    val verticalScrollState by logViewModel.verticalListState.collectAsState()
    val horizontalScrollState by logViewModel.horizontalListState.collectAsState()
    val searchFilter = logViewModel.logSearchFilterResults.collectAsState()
    val searchFilterValue = searchFilter.value

    val selectedCommit = if (selectedItem is SelectedItem.CommitBasedItem) {
        selectedItem.revCommit
    } else {
        null
    }

    LaunchedEffect(verticalScrollState, commitList) {
        launch {
            logViewModel.focusCommit.collect { commit ->
                scrollToCommit(verticalScrollState, commitList, commit)
            }
        }
        launch {
            logViewModel.scrollToUncommittedChanges.collect {
                scrollToUncommittedChanges(verticalScrollState, commitList)
            }
        }
    }

    LogDialogs(
        logViewModel,
        onResetShowLogDialog = { logViewModel.showDialog(LogDialog.None) },
        showLogDialog = showLogDialog,
        changeDefaultUpstreamBranchViewModel = changeDefaultUpstreamBranchViewModel,
    )

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        var graphPadding by remember(logViewModel) { mutableStateOf(logViewModel.graphPadding) }
        var graphWidth = (CANVAS_DEFAULT_WIDTH + graphPadding).dp

        if (graphWidth.value < CANVAS_MIN_WIDTH) graphWidth = CANVAS_MIN_WIDTH.dp

        val maxLinePosition = if (commitList.isNotEmpty())
            commitList.maxLine
        else
            MIN_GRAPH_LANES

        var graphRealWidth = ((maxLinePosition + MARGIN_GRAPH_LANES) * LANE_WIDTH).dp

        // Using remember(graphRealWidth, graphWidth) makes the selected background color glitch when changing tabs
        if (graphRealWidth < graphWidth) {
            graphRealWidth = graphWidth
        }


        if (searchFilterValue is LogSearch.SearchResults) {
            SearchFilter(
                logViewModel = logViewModel,
                searchFilterResults = searchFilterValue,
                searchFocused = { logViewModel.addSearchToCloseableView() },
            )
        }

        GraphHeader(
            graphWidth = graphWidth,
            onPaddingChange = {
                graphPadding += it
                logViewModel.graphPadding = graphPadding
            },
            onShowSearch = { scope.launch { logViewModel.onSearchValueChanged("") } }
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
                logStatus = logStatus,
                repositoryState = repositoryState,
                selectedItem = selectedItem,
                commitList = commitList,
                graphWidth = graphWidth,
                commitsLimit = logStatus.commitsLimit,
                onMerge = { ref -> logViewModel.mergeBranch(ref) },
                onRebase = { ref -> logViewModel.rebaseBranch(ref) },
                onShowLogDialog = { dialog -> logViewModel.showDialog(dialog) },
                onCheckoutCommit = { logViewModel.checkoutCommit(it) },
                onRevertCommit = { logViewModel.revertCommit(it) },
                onCherryPickCommit = { logViewModel.cherryPickCommit(it) },
                onCheckoutRemoteBranch = { logViewModel.checkoutRemoteBranch(it) },
                onCheckoutRef = { logViewModel.checkoutRef(it) },
                onRebaseInteractive = { logViewModel.rebaseInteractive(it) },
                onCommitSelected = { logViewModel.selectCommit(it) },
                onUncommittedChangesSelected = { logViewModel.selectUncommittedChanges() },
                onDeleteStash = { logViewModel.deleteStash(it) },
                onApplyStash = { logViewModel.applyStash(it) },
                onPopStash = { logViewModel.popStash(it) },
                onDeleteBranch = { logViewModel.deleteBranch(it) },
                onDeleteRemoteBranch = { logViewModel.deleteRemoteBranch(it) },
                onDeleteTag = { logViewModel.deleteTag(it) },
                onPushToRemoteBranch = { logViewModel.pushToRemoteBranch(it) },
                onPullFromRemoteBranch = { logViewModel.pullFromRemoteBranch(it) },
            )

            val density = LocalDensity.current.density
            DividerLog(
                modifier = Modifier.draggable(
                    rememberDraggableState {
                        graphPadding += it / density
                        logViewModel.graphPadding = graphPadding
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
                            painterResource(AppIcons.ALIGN_TOP),
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
    commitList: GraphCommitList,
    commit: RevCommit?,
) {
    val index = commitList.indexOfFirst { it.name == commit?.name }
    // TODO Show a message informing the user why we aren't scrolling
    // Index can be -1 if the ref points to a commit that is not shown in the graph due to the limited
    // number of displayed commits.
    if (index >= 0) verticalScrollState.scrollToItem(index)
}

suspend fun scrollToUncommittedChanges(
    verticalScrollState: LazyListState,
    commitList: GraphCommitList,
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

                        keyEvent.matchesBinding(KeybindingOption.EXIT) -> {
                            logViewModel.closeSearch()
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
                        Icon(painterResource(AppIcons.CLOSE), contentDescription = null)
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
    searchFilter: List<GraphNode>?,
    selectedCommit: RevCommit?,
    logStatus: LogStatus.Loaded,
    repositoryState: RepositoryState,
    selectedItem: SelectedItem,
    commitList: GraphCommitList,
    commitsLimit: Int,
    onCheckoutCommit: (GraphNode) -> Unit,
    onRevertCommit: (GraphNode) -> Unit,
    onCherryPickCommit: (GraphNode) -> Unit,
    onCheckoutRemoteBranch: (Ref) -> Unit,
    onCheckoutRef: (Ref) -> Unit,
    onMerge: (Ref) -> Unit,
    onRebase: (Ref) -> Unit,
    onRebaseInteractive: (GraphNode) -> Unit,
    onCommitSelected: (GraphNode) -> Unit,
    onUncommittedChangesSelected: () -> Unit,
    onDeleteStash: (GraphNode) -> Unit,
    onApplyStash: (GraphNode) -> Unit,
    onPopStash: (GraphNode) -> Unit,
    onDeleteBranch: (Ref) -> Unit,
    onDeleteRemoteBranch: (Ref) -> Unit,
    onDeleteTag: (Ref) -> Unit,
    onPushToRemoteBranch: (Ref) -> Unit,
    onPullFromRemoteBranch: (Ref) -> Unit,
    onShowLogDialog: (LogDialog) -> Unit,
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
                        .handMouseClickable { onUncommittedChangesSelected() }
                ) {
                    UncommittedChangesGraphNode(
                        hasPreviousCommits = commitList.isNotEmpty(),
                        isSelected = selectedItem is SelectedItem.UncommittedChanges,
                        modifier = Modifier.offset(-horizontalScrollState.value.dp)
                    )

                    UncommittedChangesLine(
                        graphWidth = graphWidth,
                        isSelected = selectedItem == SelectedItem.UncommittedChanges,
                        statusSummary = logStatus.statusSummary,
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
                isSelected = selectedCommit?.name == graphNode.name,
                currentBranch = logStatus.currentBranch,
                matchesSearchFilter = searchFilter?.contains(graphNode),
                horizontalScrollState = horizontalScrollState,
                showCreateNewBranch = { onShowLogDialog(LogDialog.NewBranch(graphNode)) },
                showCreateNewTag = { onShowLogDialog(LogDialog.NewTag(graphNode)) },
                resetBranch = { onShowLogDialog(LogDialog.ResetBranch(graphNode)) },
                onMergeBranch = onMerge,
                onDeleteBranch = onDeleteBranch,
                onDeleteRemoteBranch = onDeleteRemoteBranch,
                onDeleteTag = onDeleteTag,
                onPushToRemoteBranch = onPushToRemoteBranch,
                onPullFromRemoteBranch = onPullFromRemoteBranch,
                onRebaseBranch = onRebase,
                onRebaseInteractive = { onRebaseInteractive(graphNode) },
                onRevCommitSelected = { onCommitSelected(graphNode) },
                onChangeDefaultUpstreamBranch = { onShowLogDialog(LogDialog.ChangeDefaultBranch(it)) },
                onDeleteStash = { onDeleteStash(graphNode) },
                onApplyStash = { onApplyStash(graphNode) },
                onPopStash = { onPopStash(graphNode) },
                onCheckoutCommit = { onCheckoutCommit(graphNode) },
                onRevertCommit = { onRevertCommit(graphNode) },
                onCherryPickCommit = { onCherryPickCommit(graphNode) },
                onCheckoutRemoteBranch = onCheckoutRemoteBranch,
                onCheckoutRef = onCheckoutRef,
            )
        }

        if (commitsLimit >= 0 && commitsLimit <= commitList.count()) {
            item {
                Box(
                    modifier = Modifier
                        .padding(start = graphWidth + 24.dp)
                        .height(MaterialTheme.linesHeight.logCommitHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "The commits list has been limited to $commitsLimit. Access the settings to change it.",
                        color = MaterialTheme.colors.onBackground,
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                    )
                }
            }
        }

        item {
            Box(modifier = Modifier.height(LOG_BOTTOM_PADDING.dp))
        }
    }
}

@Composable
fun LogDialogs(
    logViewModel: LogViewModel,
    onResetShowLogDialog: () -> Unit,
    changeDefaultUpstreamBranchViewModel: () -> ChangeDefaultUpstreamBranchViewModel,
    showLogDialog: LogDialog,
) {
    when (showLogDialog) {
        is LogDialog.NewBranch -> {
            NewBranchDialog(onClose = onResetShowLogDialog, onAccept = { branchName ->
                logViewModel.createBranchOnCommit(branchName, showLogDialog.graphNode)
                onResetShowLogDialog()
            })
        }

        is LogDialog.NewTag -> {
            NewTagDialog(onReject = onResetShowLogDialog, onAccept = { tagName ->
                logViewModel.createTagOnCommit(tagName, showLogDialog.graphNode)
                onResetShowLogDialog()
            })
        }

        is LogDialog.ResetBranch -> ResetBranchDialog(onReject = onResetShowLogDialog, onAccept = { resetType ->
            logViewModel.resetToCommit(showLogDialog.graphNode, resetType)
            onResetShowLogDialog()
        })

        LogDialog.None -> {
        }

        is LogDialog.ChangeDefaultBranch -> {
            SetDefaultUpstreamBranchDialog(
                viewModel = changeDefaultUpstreamBranchViewModel(),
                branch = showLogDialog.ref,
                onClose = { onResetShowLogDialog() },
            )
        }
    }
}

@Composable
fun GraphHeader(
    graphWidth: Dp,
    onPaddingChange: (Float) -> Unit,
    onShowSearch: () -> Unit
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
                    painterResource(AppIcons.SEARCH),
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
    count: Int, icon: ImageVector, color: Color
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
    graphNode: GraphNode,
    isSelected: Boolean,
    currentBranch: Ref?,
    matchesSearchFilter: Boolean?,
    showCreateNewBranch: () -> Unit,
    showCreateNewTag: () -> Unit,
    resetBranch: () -> Unit,
    onApplyStash: () -> Unit,
    onPopStash: () -> Unit,
    onDeleteStash: () -> Unit,
    onMergeBranch: (Ref) -> Unit,
    onDeleteBranch: (Ref) -> Unit,
    onDeleteRemoteBranch: (Ref) -> Unit,
    onDeleteTag: (Ref) -> Unit,
    onPushToRemoteBranch: (Ref) -> Unit,
    onPullFromRemoteBranch: (Ref) -> Unit,
    onRebaseBranch: (Ref) -> Unit,
    onRevCommitSelected: () -> Unit,
    onRebaseInteractive: () -> Unit,
    onCheckoutCommit: () -> Unit,
    onRevertCommit: () -> Unit,
    onCherryPickCommit: () -> Unit,
    onCheckoutRemoteBranch: (Ref) -> Unit,
    onCheckoutRef: (Ref) -> Unit,
    onChangeDefaultUpstreamBranch: (Ref) -> Unit,
    horizontalScrollState: ScrollState,
) {
    val isLastCommitOfCurrentBranch = currentBranch?.objectId?.name == graphNode.id.name

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
            val nodeColor = colors[graphNode.lane.position % colors.size]

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
                        commit = graphNode,
                        nodeColor = nodeColor,
                        matchesSearchFilter = matchesSearchFilter,
                        currentBranch = currentBranch,
                        onCheckoutRef = { ref ->
                            if (ref.isRemote && ref.isBranch) {
                                onCheckoutRemoteBranch(ref)
                            } else {
                                onCheckoutRef(ref)
                            }
                        },
                        onMergeBranch = { ref -> onMergeBranch(ref) },
                        onDeleteBranch = { ref -> onDeleteBranch(ref) },
                        onDeleteRemoteBranch = { ref -> onDeleteRemoteBranch(ref) },
                        onDeleteTag = { ref -> onDeleteTag(ref) },
                        onRebaseBranch = { ref -> onRebaseBranch(ref) },
                        onPushRemoteBranch = { ref -> onPushToRemoteBranch(ref) },
                        onPullRemoteBranch = { ref -> onPullFromRemoteBranch(ref) },
                        onChangeDefaultUpstreamBranch = { ref -> onChangeDefaultUpstreamBranch(ref) },
                    )
                }
            }
        }
    }
}

@Composable
fun CommitMessage(
    commit: GraphNode,
    currentBranch: Ref?,
    nodeColor: Color,
    matchesSearchFilter: Boolean?,
    onCheckoutRef: (ref: Ref) -> Unit,
    onMergeBranch: (ref: Ref) -> Unit,
    onDeleteBranch: (ref: Ref) -> Unit,
    onDeleteRemoteBranch: (ref: Ref) -> Unit,
    onRebaseBranch: (ref: Ref) -> Unit,
    onDeleteTag: (ref: Ref) -> Unit,
    onPushRemoteBranch: (ref: Ref) -> Unit,
    onPullRemoteBranch: (ref: Ref) -> Unit,
    onChangeDefaultUpstreamBranch: (ref: Ref) -> Unit,
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
            if (!commit.isStash) {
                commit.refs.sortedWith { ref1, ref2 ->
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
                        )
                    }
                }
            }
        }

        val message = remember(commit.id.name) {
            commit.getShortMessageTrimmed()
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

        Text(
            text = commit.authorIdent.`when`.toSmartSystemString(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onBackgroundSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    plotCommit: GraphNode,
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

        val itemPosition = plotCommit.lane.position

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {
                if (plotCommit.childCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), 0f),
                        strokeWidth = 2f,
                    )
                }

                forkingOffLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (plotLane.position + 1), 0f),
                        strokeWidth = 2f,
                    )
                }

                mergingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane.position + 1), this.size.height),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        strokeWidth = 2f,
                    )
                }

                if (plotCommit.parentCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.size.height),
                        strokeWidth = 2f,
                    )
                }

                passingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane.position + 1), 0f),
                        end = Offset(laneWidthWithDensity * (plotLane.position + 1), this.size.height),
                        strokeWidth = 2f,
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
    plotCommit: GraphNode,
    color: Color,
) {
    val author = plotCommit.authorIdent
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
                painterResource(AppIcons.STASH),
                modifier = Modifier.size(20.dp),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
            )
        }
    } else {
        InstantTooltip(
            "${author.name} <${author.emailAddress}>",
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
                    personIdent = plotCommit.authorIdent,
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
                    strokeWidth = 2f,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BranchChip(
    modifier: Modifier = Modifier,
    isCurrentBranch: Boolean = false,
    ref: Ref,
    currentBranch: Ref?,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onDeleteRemoteBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onPushRemoteBranch: () -> Unit,
    onPullRemoteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
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
        )
    }

    var endingContent: @Composable () -> Unit = {}
    if (isCurrentBranch) {
        endingContent = {
            Icon(
                painter = painterResource(AppIcons.LOCATION),
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
                tint = MaterialTheme.colors.primaryVariant,
            )
        }
    }

    RefChip(
        modifier = modifier,
        color = color,
        ref = ref,
        icon = AppIcons.BRANCH,
        onCheckoutRef = onCheckoutBranch,
        contextMenuItemsList = contextMenuItemsList,
        endingContent = endingContent,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    ref: Ref,
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

    RefChip(
        modifier,
        ref,
        AppIcons.TAG,
        onCheckoutRef = onCheckoutTag,
        contextMenuItemsList = contextMenuItemsList,
        color = color,
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RefChip(
    modifier: Modifier = Modifier,
    ref: Ref,
    icon: String,
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
                    text = ref.simpleLogName,
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