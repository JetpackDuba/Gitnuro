@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package app.ui.log

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.extensions.*
import app.git.StatusSummary
import app.git.graph.GraphCommitList
import app.git.graph.GraphNode
import app.keybindings.KeybindingOption
import app.keybindings.matchesBinding
import app.theme.*
import app.ui.SelectedItem
import app.ui.components.AvatarImage
import app.ui.components.ScrollableLazyColumn
import app.ui.context_menu.branchContextMenuItems
import app.ui.context_menu.logContextMenu
import app.ui.context_menu.tagContextMenuItems
import app.ui.dialogs.NewBranchDialog
import app.ui.dialogs.NewTagDialog
import app.ui.dialogs.ResetBranchDialog
import app.viewmodels.LogSearch
import app.viewmodels.LogStatus
import app.viewmodels.LogViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import java.awt.Cursor

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

/**
 * Additional number of lanes to simulate to create a margin at the end of the graph.
 */
private const val MARGIN_GRAPH_LANES = 2
private const val LANE_WIDTH = 30f
private const val DIVIDER_WIDTH = 8

private const val LINE_HEIGHT = 40
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
) {
    val scope = rememberCoroutineScope()
    val logStatusState = logViewModel.logStatus.collectAsState()
    val logStatus = logStatusState.value
    val showLogDialog by logViewModel.logDialog.collectAsState()


    val selectedCommit = if (selectedItem is SelectedItem.CommitBasedItem) {
        selectedItem.revCommit
    } else {
        null
    }

    if (logStatus is LogStatus.Loaded) {
        val hasUncommitedChanges = logStatus.hasUncommitedChanges
        val commitList = logStatus.plotCommitList
        val verticalScrollState by logViewModel.verticalListState.collectAsState()
        val horizontalScrollState by logViewModel.horizontalListState.collectAsState()
        val searchFilter = logViewModel.logSearchFilterResults.collectAsState()
        val searchFilterValue = searchFilter.value
        // With this method, whenever the scroll changes, the log is recomposed and the graph list is updated with
        // the proper scroll position
        verticalScrollState.observeScrollChanges()

        LaunchedEffect(selectedCommit) {
            // Scroll to commit if a Ref is selected
            if (selectedItem is SelectedItem.Ref) {
                scrollToCommit(verticalScrollState, commitList, selectedCommit)
            }
        }

        LaunchedEffect(Unit) {
            logViewModel.focusCommit.collect { commit ->
                scrollToCommit(verticalScrollState, commitList, commit)
            }
        }

        LogDialogs(
            logViewModel,
            onResetShowLogDialog = { logViewModel.showDialog(LogDialog.None) },
            showLogDialog = showLogDialog,
        )

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .fillMaxSize()
        ) {
            var graphPadding by remember(logViewModel) { mutableStateOf(logViewModel.graphPadding) }
            var graphWidth = (CANVAS_DEFAULT_WIDTH + graphPadding).dp

            if (graphWidth.value < CANVAS_MIN_WIDTH) graphWidth = CANVAS_MIN_WIDTH.dp

            if (searchFilterValue is LogSearch.SearchResults) {
                SearchFilter(logViewModel, searchFilterValue)
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
                GraphList(
                    commitList = commitList,
                    selectedCommit = selectedCommit,
                    selectedItem = selectedItem,
                    horizontalScrollState = horizontalScrollState,
                    graphWidth = graphWidth,
                    verticalScrollState = verticalScrollState,
                    hasUncommitedChanges = hasUncommitedChanges,
                    commitsLimit = logStatus.commitsLimit,
                )

                // The commits' messages list overlaps with the graph list to catch all the click events but leaves
                // a padding, so it doesn't cover the graph
                MessagesList(
                    scrollState = verticalScrollState,
                    hasUncommitedChanges = hasUncommitedChanges,
                    searchFilter = if (searchFilterValue is LogSearch.SearchResults) searchFilterValue.commits else null,
                    selectedCommit = selectedCommit,
                    logStatus = logStatus,
                    repositoryState = repositoryState,
                    selectedItem = selectedItem,
                    commitList = commitList,
                    logViewModel = logViewModel,
                    graphWidth = graphWidth,
                    commitsLimit = logStatus.commitsLimit,
                    onMerge = { ref ->
                        logViewModel.mergeBranch(ref)
                    },
                    onRebase = { ref ->
                        logViewModel.rebaseBranch(ref)
                    },
                    onShowLogDialog = { dialog ->
                        logViewModel.showDialog(dialog)
                    }
                )

                val density = LocalDensity.current.density
                DividerLog(
                    modifier = Modifier.draggable(
                        rememberDraggableState {
                            graphPadding += it * density
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

                if (verticalScrollState.firstVisibleItemIndex > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp)
                            .clip(RoundedCornerShape(50))
                            .handMouseClickable {
                                scope.launch {
                                    verticalScrollState.scrollToItem(0)
                                }
                            }
                            .background(MaterialTheme.colors.primary)
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource("align_top.svg"),
                                contentDescription = null,
                                tint = MaterialTheme.colors.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )

                            Text(
                                text = "Scroll to top",
                                modifier = Modifier.padding(start = 8.dp),
                                color = MaterialTheme.colors.onPrimary,
                                style = MaterialTheme.typography.body1,
                            )
                        }
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

@Composable
fun SearchFilter(
    logViewModel: LogViewModel,
    searchFilterResults: LogSearch.SearchResults
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
            .height(64.dp),
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
                .onPreviewKeyEvent { keyEvent ->
                    when {
                        keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && keyEvent.type == KeyEventType.KeyUp -> {
                            scope.launch {
                                logViewModel.selectNextFilterCommit()
                            }
                            true
                        }
                        keyEvent.matchesBinding(KeybindingOption.EXIT) && keyEvent.type == KeyEventType.KeyUp -> {
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
                            color = MaterialTheme.colors.secondaryTextColor,
                        )
                    }

                    IconButton(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Hand),
                        onClick = {
                            scope.launch { logViewModel.selectPreviousFilterCommit() }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                    }

                    IconButton(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Hand),
                        onClick = {
                            scope.launch { logViewModel.selectNextFilterCommit() }
                        }
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }

                    IconButton(
                        modifier = Modifier
                            .pointerHoverIcon(PointerIconDefaults.Hand)
                            .padding(end = 4.dp),
                        onClick = { logViewModel.closeSearch() }
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            }
        )
    }
}

@Composable
fun MessagesList(
    scrollState: LazyListState,
    hasUncommitedChanges: Boolean,
    searchFilter: List<GraphNode>?,
    selectedCommit: RevCommit?,
    logStatus: LogStatus.Loaded,
    repositoryState: RepositoryState,
    selectedItem: SelectedItem,
    commitList: GraphCommitList,
    logViewModel: LogViewModel,
    commitsLimit: Int,
    onMerge: (Ref) -> Unit,
    onRebase: (Ref) -> Unit,
    onShowLogDialog: (LogDialog) -> Unit,
    graphWidth: Dp,
) {
    ScrollableLazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (
            hasUncommitedChanges ||
            repositoryState.isMerging ||
            repositoryState.isRebasing ||
            repositoryState.isCherryPicking
        ) {
            item {
                UncommitedChangesLine(
                    graphWidth = graphWidth,
                    isSelected = selectedItem == SelectedItem.UncommitedChanges,
                    statusSummary = logStatus.statusSummary,
                    repositoryState = repositoryState,
                    onUncommitedChangesSelected = {
                        logViewModel.selectUncommitedChanges()
                    }
                )
            }
        }
        items(items = commitList) { graphNode ->
            CommitLine(
                graphWidth = graphWidth,
                logViewModel = logViewModel,
                graphNode = graphNode,
                isSelected = selectedCommit?.name == graphNode.name,
                currentBranch = logStatus.currentBranch,
                matchesSearchFilter = searchFilter?.contains(graphNode),
                showCreateNewBranch = { onShowLogDialog(LogDialog.NewBranch(graphNode)) },
                showCreateNewTag = { onShowLogDialog(LogDialog.NewTag(graphNode)) },
                resetBranch = { onShowLogDialog(LogDialog.ResetBranch(graphNode)) },
                onMergeBranch = onMerge,
                onRebaseBranch = onRebase,
                onRebaseInteractive = { logViewModel.rebaseInteractive(graphNode) },
                onRevCommitSelected = { logViewModel.selectLogLine(graphNode) },
            )
        }

        if (commitsLimit >= 0 && commitsLimit <= commitList.count()) {
            item {
                Box(
                    modifier = Modifier
                        .padding(start = graphWidth + 24.dp)
                        .height(LINE_HEIGHT.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "The commits list has been limited to $commitsLimit. Access the settings to change it.",
                        color = MaterialTheme.colors.primaryTextColor,
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
fun GraphList(
    commitList: GraphCommitList,
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState,
    graphWidth: Dp,
    hasUncommitedChanges: Boolean,
    selectedCommit: RevCommit?,
    selectedItem: SelectedItem,
    commitsLimit: Int,
) {
    val maxLinePosition = if (commitList.isNotEmpty())
        commitList.maxLine
    else
        MIN_GRAPH_LANES

    var graphRealWidth = ((maxLinePosition + MARGIN_GRAPH_LANES) * LANE_WIDTH).dp

    // Using remember(graphRealWidth, graphWidth) makes the selected background color glitch when changing tabs
    if (graphRealWidth < graphWidth) {
        graphRealWidth = graphWidth
    }

    Box(
        Modifier
            .width(graphWidth)
            .fillMaxHeight()
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState)
                .padding(bottom = 8.dp)
        ) {
            LazyColumn(
                state = verticalScrollState, modifier = Modifier.width(graphRealWidth)
            ) {
                if (hasUncommitedChanges) {
                    item {
                        Row(
                            modifier = Modifier
                                .height(LINE_HEIGHT.dp)
                                .fillMaxWidth(),
                        ) {
                            UncommitedChangesGraphNode(
                                modifier = Modifier.fillMaxSize(),
                                hasPreviousCommits = commitList.isNotEmpty(),
                                isSelected = selectedItem is SelectedItem.UncommitedChanges,
                            )
                        }
                    }
                }

                items(items = commitList) { graphNode ->
                    val nodeColor = colors[graphNode.lane.position % colors.size]

                    Row(
                        modifier = Modifier
                            .height(LINE_HEIGHT.dp)
                            .fillMaxWidth(),
                    ) {
                        CommitsGraphLine(
                            modifier = Modifier.fillMaxSize(),
                            plotCommit = graphNode,
                            nodeColor = nodeColor,
                            isSelected = selectedCommit?.name == graphNode.name,
                        )
                    }
                }

                // Spacing when the commits limit is present
                if (commitsLimit >= 0 && commitsLimit <= commitList.count()) {
                    item {
                        Box(
                            modifier = Modifier
                                .height(LINE_HEIGHT.dp),
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.height(LOG_BOTTOM_PADDING.dp))
                }
            }
        }
    }
}

@Composable
fun LogDialogs(
    logViewModel: LogViewModel,
    onResetShowLogDialog: () -> Unit,
    showLogDialog: LogDialog,
) {
    when (showLogDialog) {
        is LogDialog.NewBranch -> {
            NewBranchDialog(onReject = onResetShowLogDialog, onAccept = { branchName ->
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
                .height(40.dp)
                .background(MaterialTheme.colors.headerBackground),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .width(graphWidth)
                    .padding(start = 16.dp),
                text = "Graph",
                color = MaterialTheme.colors.headerText,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            val density = LocalDensity.current.density

            SimpleDividerLog(
                modifier = Modifier.draggable(
                    rememberDraggableState {
                        onPaddingChange(it * density) // Multiply by density for screens with scaling > 1
                    }, Orientation.Horizontal
                ),
            )

            Text(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
                text = "Message",
                color = MaterialTheme.colors.headerText,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            IconButton(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .pointerHoverIcon(PointerIconDefaults.Hand),
                onClick = onShowSearch
            ) {
                Icon(
                    Icons.Default.Search,
                    modifier = Modifier.size(18.dp),
                    contentDescription = null,
                    tint = MaterialTheme.colors.primaryTextColor,
                )
            }
        }
    }
}

@Composable
fun UncommitedChangesLine(
    graphWidth: Dp,
    isSelected: Boolean,
    repositoryState: RepositoryState,
    statusSummary: StatusSummary,
    onUncommitedChangesSelected: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .clickable { onUncommitedChangesSelected() }
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
            else -> "Uncommited changes"
        }

        Text(
            text = text,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.body2,
            maxLines = 1,
            color = MaterialTheme.colors.primaryTextColor,
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
        )

        Icon(
            imageVector = icon, tint = color, contentDescription = null, modifier = Modifier.size(14.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommitLine(
    graphWidth: Dp,
    logViewModel: LogViewModel,
    graphNode: GraphNode,
    isSelected: Boolean,
    currentBranch: Ref?,
    matchesSearchFilter: Boolean?,
    showCreateNewBranch: () -> Unit,
    showCreateNewTag: () -> Unit,
    resetBranch: () -> Unit,
    onMergeBranch: (Ref) -> Unit,
    onRebaseBranch: (Ref) -> Unit,
    onRevCommitSelected: () -> Unit,
    onRebaseInteractive: () -> Unit,
) {
    ContextMenuArea(
        items = {
            logContextMenu(
                onCheckoutCommit = { logViewModel.checkoutCommit(graphNode) },
                onCreateNewBranch = showCreateNewBranch,
                onCreateNewTag = showCreateNewTag,
                onRevertCommit = { logViewModel.revertCommit(graphNode) },
                onCherryPickCommit = { logViewModel.cherrypickCommit(graphNode) },
                onRebaseInteractive = onRebaseInteractive,
                onResetBranch = { resetBranch() },
            )
        },
    ) {
        Box(
            modifier = Modifier
                .clickable { onRevCommitSelected() }
                .padding(start = graphWidth)
                .height(LINE_HEIGHT.dp)
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
                val nodeColor = colors[graphNode.lane.position % colors.size]
                CommitMessage(
                    commit = graphNode,
                    refs = graphNode.refs,
                    nodeColor = nodeColor,
                    matchesSearchFilter = matchesSearchFilter,
                    currentBranch = currentBranch,
                    onCheckoutRef = { ref -> logViewModel.checkoutRef(ref) },
                    onMergeBranch = { ref -> onMergeBranch(ref) },
                    onDeleteBranch = { ref -> logViewModel.deleteBranch(ref) },
                    onDeleteRemoteBranch = { ref -> logViewModel.deleteRemoteBranch(ref) },
                    onDeleteTag = { ref -> logViewModel.deleteTag(ref) },
                    onRebaseBranch = { ref -> onRebaseBranch(ref) },
                    onPushRemoteBranch = { ref -> logViewModel.pushToRemoteBranch(ref) },
                    onPullRemoteBranch = { ref -> logViewModel.pullFromRemoteBranch(ref) },
                )
            }
        }
    }
}

@Composable
fun CommitMessage(
    commit: RevCommit,
    refs: List<Ref>,
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
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp)
        ) {
            refs.sortedWith { ref1, ref2 ->
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
                    )
                }
            }
        }
        Text(
            text = commit.shortMessage,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
            style = MaterialTheme.typography.body2,
            color = if (matchesSearchFilter == false) MaterialTheme.colors.secondaryTextColor else MaterialTheme.colors.primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = commit.authorIdent.`when`.toSmartSystemString(),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.secondaryTextColor,
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
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
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
fun CommitsGraphLine(
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
                    )
                }

                forkingOffLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (plotLane.position + 1), 0f),
                    )
                }

                mergingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane.position + 1), this.size.height),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                    )
                }

                if (plotCommit.parentCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(laneWidthWithDensity * (itemPosition + 1), this.center.y),
                        end = Offset(laneWidthWithDensity * (itemPosition + 1), this.size.height),
                    )
                }

                passingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(laneWidthWithDensity * (plotLane.position + 1), 0f),
                        end = Offset(laneWidthWithDensity * (plotLane.position + 1), this.size.height),
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

@Composable
fun UncommitedChangesGraphNode(
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
        )
    }

    var endingContent: @Composable () -> Unit = {}
    if (isCurrentBranch) {
        endingContent = {
            Icon(
                painter = painterResource("location.svg"),
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
        icon = "branch.svg",
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
        "tag.svg",
        onCheckoutRef = onCheckoutTag,
        contextMenuItemsList = contextMenuItemsList,
        color = color,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RefChip(
    modifier: Modifier = Modifier,
    ref: Ref,
    icon: String,
    color: Color,
    onCheckoutRef: () -> Unit,
    contextMenuItemsList: () -> List<ContextMenuItem>,
    endingContent: @Composable () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = color, shape = RoundedCornerShape(16.dp))
            .combinedClickable(onDoubleClick = onCheckoutRef, onClick = {})
            .pointerHoverIcon(PointerIconDefaults.Hand)
    ) {
        ContextMenuArea(
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
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                endingContent()
            }
        }
    }
}