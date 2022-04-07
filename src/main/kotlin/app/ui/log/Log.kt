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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.*
import app.git.StatusSummary
import app.git.graph.GraphCommitList
import app.git.graph.GraphNode
import app.theme.*
import app.ui.SelectedItem
import app.ui.components.AvatarImage
import app.ui.components.ScrollableLazyColumn
import app.ui.context_menu.branchContextMenuItems
import app.ui.context_menu.logContextMenu
import app.ui.context_menu.tagContextMenuItems
import app.ui.dialogs.*
import app.viewmodels.LogSearch
import app.viewmodels.LogStatus
import app.viewmodels.LogViewModel
import kotlinx.coroutines.flow.collect
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
private const val MIN_GRAPH_LANES = 2

/**
 * Additional number of lanes to simulate to create a margin at the end of the graph.
 */
private const val MARGIN_GRAPH_LANES = 2
private const val LANE_WIDTH = 30f
private const val DIVIDER_WIDTH = 8

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
    val showLogDialog = remember { mutableStateOf<LogDialog>(LogDialog.None) }

    val selectedCommit = if (selectedItem is SelectedItem.CommitBasedItem) {
        selectedItem.revCommit
    } else {
        null
    }

    if (logStatus is LogStatus.Loaded) {
        val hasUncommitedChanges = logStatus.hasUncommitedChanges
        val commitList = logStatus.plotCommitList
        val verticalScrollState = rememberLazyListState()
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
            currentBranch = logStatus.currentBranch,
            onResetShowLogDialog = { showLogDialog.value = LogDialog.None },
            showLogDialog = showLogDialog.value,
        )

        Column(
            modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()
        ) {
            val weightMod = remember { mutableStateOf(0f) }
            var graphWidth = (CANVAS_MIN_WIDTH + weightMod.value).dp

            if (graphWidth.value < CANVAS_MIN_WIDTH) graphWidth = CANVAS_MIN_WIDTH.dp

            if (searchFilterValue is LogSearch.SearchResults) {
                SearchFilter(logViewModel, searchFilterValue)
            }
            GraphHeader(
                graphWidth = graphWidth,
                weightMod = weightMod,
                onShowSearch = { scope.launch { logViewModel.onSearchValueChanged("") } }
            )

            val horizontalScrollState = rememberScrollState(0)
            Box {
                GraphList(
                    commitList = commitList,
                    stateHorizontal = horizontalScrollState,
                    graphWidth = graphWidth,
                    scrollState = verticalScrollState,
                    hasUncommitedChanges = hasUncommitedChanges,
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
                    onShowLogDialog = { dialog ->
                        showLogDialog.value = dialog
                    })

                DividerLog(
                    modifier = Modifier.draggable(
                        rememberDraggableState {
                            weightMod.value += it
                        }, Orientation.Horizontal
                    ),
                    graphWidth = graphWidth,
                )

                // Scrollbar used to scroll horizontally the graph nodes
                // Added after every component to have the highest priority when clicking
                HorizontalScrollbar(
                    modifier = Modifier.align(Alignment.BottomStart).width(graphWidth)
                        .padding(start = 4.dp, bottom = 4.dp), style = LocalScrollbarStyle.current.copy(
                        unhoverColor = MaterialTheme.colors.scrollbarUnhover,
                        hoverColor = MaterialTheme.colors.scrollbarHover,
                    ), adapter = rememberScrollbarAdapter(horizontalScrollState)
                )
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
            .height(64.dp)
            .background(MaterialTheme.colors.graphHeaderBackground),
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
                .onPreviewKeyEvent {
                    when {
                        it.key == Key.Enter && it.type == KeyEventType.KeyUp -> {
                            scope.launch {
                                logViewModel.selectNextFilterCommit()
                            }
                            true
                        }
                        it.key == Key.Escape && it.type == KeyEventType.KeyUp -> {
                            logViewModel.closeSearch()
                            true
                        }
                        else -> false
                    }
                },
            label = {
                Text("Search by message, author name or commit ID")
            },
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
            textStyle = TextStyle.Default.copy(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
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
    onShowLogDialog: (LogDialog) -> Unit,
    graphWidth: Dp,
) {
    ScrollableLazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (hasUncommitedChanges) item {
            UncommitedChangesLine(graphWidth = graphWidth,
                selected = selectedItem == SelectedItem.UncommitedChanges,
                statusSummary = logStatus.statusSummary,
                repositoryState = repositoryState,
                onUncommitedChangesSelected = {
                    logViewModel.selectUncommitedChanges()
                })
        }
        items(items = commitList) { graphNode ->
            CommitLine(
                graphWidth = graphWidth,
                logViewModel = logViewModel,
                graphNode = graphNode,
                selected = selectedCommit?.name == graphNode.name,
                currentBranch = logStatus.currentBranch,
                matchesSearchFilter = searchFilter?.contains(graphNode),
                showCreateNewBranch = { onShowLogDialog(LogDialog.NewBranch(graphNode)) },
                showCreateNewTag = { onShowLogDialog(LogDialog.NewTag(graphNode)) },
                resetBranch = { onShowLogDialog(LogDialog.ResetBranch(graphNode)) },
                onMergeBranch = { ref -> onShowLogDialog(LogDialog.MergeBranch(ref)) },
                onRebaseBranch = { ref -> onShowLogDialog(LogDialog.RebaseBranch(ref)) },
                onRevCommitSelected = { logViewModel.selectLogLine(graphNode) },
            )
        }

        item {
            Box(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun GraphList(
    commitList: GraphCommitList,
    stateHorizontal: ScrollState = rememberScrollState(0),
    graphWidth: Dp,
    scrollState: LazyListState,
    hasUncommitedChanges: Boolean,
) {
    val maxLinePosition = if (commitList.isNotEmpty())
        commitList.maxLine
    else
        MIN_GRAPH_LANES

    val graphRealWidth = ((maxLinePosition + MARGIN_GRAPH_LANES) * LANE_WIDTH).dp

    Box(
        Modifier.width(graphWidth).fillMaxHeight()
    ) {

        Box(
            modifier = Modifier.fillMaxSize().horizontalScroll(stateHorizontal).padding(bottom = 8.dp)
        ) {
            LazyColumn(
                state = scrollState, modifier = Modifier.width(graphRealWidth)
            ) {
                if (hasUncommitedChanges) {
                    item {
                        Row(
                            modifier = Modifier.height(40.dp).fillMaxWidth(),
                        ) {
                            UncommitedChangesGraphNode(
                                modifier = Modifier.width(graphWidth),
                                hasPreviousCommits = commitList.isNotEmpty(),
                            )
                        }
                    }
                }

                items(items = commitList) { graphNode ->
                    val nodeColor = colors[graphNode.lane.position % colors.size]
                    Row(
                        modifier = Modifier.height(40.dp).fillMaxWidth(),
                    ) {
                        CommitsGraphLine(
                            modifier = Modifier.fillMaxHeight(),
                            plotCommit = graphNode,
                            nodeColor = nodeColor,
                        )
                    }
                }

                item {
                    Box(modifier = Modifier.height(20.dp))
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
    currentBranch: Ref?,
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
        is LogDialog.MergeBranch -> {
            if (currentBranch != null) MergeDialog(currentBranchName = currentBranch.simpleName,
                mergeBranchName = showLogDialog.ref.simpleName,
                onReject = onResetShowLogDialog,
                onAccept = { ff ->
                    logViewModel.mergeBranch(showLogDialog.ref, ff)
                    onResetShowLogDialog()
                })
        }
        is LogDialog.ResetBranch -> ResetBranchDialog(onReject = onResetShowLogDialog, onAccept = { resetType ->
            logViewModel.resetToCommit(showLogDialog.graphNode, resetType)
            onResetShowLogDialog()
        })
        is LogDialog.RebaseBranch -> {
            if (currentBranch != null) {
                RebaseDialog(currentBranchName = currentBranch.simpleName,
                    rebaseBranchName = showLogDialog.ref.simpleName,
                    onReject = onResetShowLogDialog,
                    onAccept = {
                        logViewModel.rebaseBranch(showLogDialog.ref)
                        onResetShowLogDialog()
                    })
            }
        }
        LogDialog.None -> {
        }
    }
}

@Composable
fun GraphHeader(
    graphWidth: Dp,
    weightMod: MutableState<Float>,
    onShowSearch: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).background(MaterialTheme.colors.graphHeaderBackground),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.width(graphWidth).padding(start = 8.dp),
                text = "Graph",
                color = MaterialTheme.colors.headerText,
                fontSize = 14.sp,
                maxLines = 1,
            )

            SimpleDividerLog(
                modifier = Modifier.draggable(
                    rememberDraggableState {
                        weightMod.value += it
                    }, Orientation.Horizontal
                ),
            )

            Text(
                modifier = Modifier.padding(start = 8.dp).weight(1f),
                text = "Message",
                color = MaterialTheme.colors.headerText,
                fontSize = 14.sp,
                maxLines = 1,
            )

            IconButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = onShowSearch
            ) {
                Icon(
                    Icons.Default.Search,
                    modifier = Modifier.size(24.dp),
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
    selected: Boolean,
    repositoryState: RepositoryState,
    statusSummary: StatusSummary,
    onUncommitedChangesSelected: () -> Unit,
) {
    val textColor = if (selected) {
        MaterialTheme.colors.primary
    } else MaterialTheme.colors.primaryTextColor

    Row(
        modifier = Modifier.height(40.dp).fillMaxWidth().clickable {
            onUncommitedChangesSelected()
        }.padding(
            start = graphWidth + DIVIDER_WIDTH.dp,
            end = 4.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val text = when {
            repositoryState.isRebasing -> "Pending changes to rebase"
            repositoryState.isMerging -> "Pending changes to merge"
            else -> "Uncommited changes"
        }

        Text(
            text = text,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(start = 16.dp),
            fontSize = 14.sp,
            maxLines = 1,
            color = textColor,
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
            color = MaterialTheme.colors.primaryTextColor,
            fontSize = 14.sp,
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
    selected: Boolean,
    currentBranch: Ref?,
    matchesSearchFilter: Boolean?,
    showCreateNewBranch: () -> Unit,
    showCreateNewTag: () -> Unit,
    resetBranch: () -> Unit,
    onMergeBranch: (Ref) -> Unit,
    onRebaseBranch: (Ref) -> Unit,
    onRevCommitSelected: () -> Unit,
) {
    ContextMenuArea(
        items = {
            logContextMenu(
                onCheckoutCommit = { logViewModel.checkoutCommit(graphNode) },
                onCreateNewBranch = showCreateNewBranch,
                onCreateNewTag = showCreateNewTag,
                onRevertCommit = { logViewModel.revertCommit(graphNode) },
                onCherryPickCommit = { logViewModel.cherrypickCommit(graphNode) },
                onResetBranch = { resetBranch() },
            )
        },
    ) {
        Box(
            modifier = Modifier.clickable {
                onRevCommitSelected()
            }.padding(start = graphWidth)
                .height(40.dp)
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
                    selected = selected,
                    refs = graphNode.refs,
                    nodeColor = nodeColor,
                    matchesSearchFilter = matchesSearchFilter,
                    currentBranch = currentBranch,
                    onCheckoutRef = { ref -> logViewModel.checkoutRef(ref) },
                    onMergeBranch = { ref -> onMergeBranch(ref) },
                    onDeleteBranch = { ref -> logViewModel.deleteBranch(ref) },
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
    selected: Boolean,
    refs: List<Ref>,
    currentBranch: Ref?,
    nodeColor: Color,
    matchesSearchFilter: Boolean?,
    onCheckoutRef: (ref: Ref) -> Unit,
    onMergeBranch: (ref: Ref) -> Unit,
    onDeleteBranch: (ref: Ref) -> Unit,
    onRebaseBranch: (ref: Ref) -> Unit,
    onDeleteTag: (ref: Ref) -> Unit,
    onPushRemoteBranch: (ref: Ref) -> Unit,
    onPullRemoteBranch: (ref: Ref) -> Unit,
) {
    val textColor = if (selected) {
        MaterialTheme.colors.primary
    } else MaterialTheme.colors.primaryTextColor

    val secondaryTextColor = if (selected) {
        MaterialTheme.colors.primary
    } else MaterialTheme.colors.secondaryTextColor

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
            fontSize = 14.sp,
            color = if (matchesSearchFilter == false) secondaryTextColor else textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = commit.committerIdent.`when`.toSmartSystemString(),
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 12.sp,
            color = secondaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DividerLog(modifier: Modifier, graphWidth: Dp) {
    Box(
        modifier = Modifier
            .padding(start = graphWidth)
            .width(DIVIDER_WIDTH.dp)
            .then(modifier)
            .pointerHoverIcon(PointerIconDefaults.Hand)
    ) {
        Box(
            modifier = Modifier.fillMaxHeight().width(1.dp).background(color = MaterialTheme.colors.primary)
                .align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SimpleDividerLog(modifier: Modifier) {
    DividerLog(modifier, graphWidth = 0.dp)
}


@Composable
fun CommitsGraphLine(
    modifier: Modifier = Modifier,
    plotCommit: GraphNode,
    nodeColor: Color,
) {
    val passingLanes = plotCommit.passingLanes
    val forkingOffLanes = plotCommit.forkingOffLanes
    val mergingLanes = plotCommit.mergingLanes

    Box(modifier = modifier) {
        val itemPosition = plotCommit.lane.position

        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {
                if (plotCommit.childCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(30f * (itemPosition + 1), this.center.y),
                        end = Offset(30f * (itemPosition + 1), 0f),
                    )
                }

                forkingOffLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(30f * (itemPosition + 1), this.center.y),
                        end = Offset(30f * (plotLane.position + 1), 0f),
                    )
                }

                mergingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(30f * (plotLane.position + 1), this.size.height),
                        end = Offset(30f * (itemPosition + 1), this.center.y),
                    )
                }

                if (plotCommit.parentCount > 0) {
                    drawLine(
                        color = colors[itemPosition % colors.size],
                        start = Offset(30f * (itemPosition + 1), this.center.y),
                        end = Offset(30f * (itemPosition + 1), this.size.height),
                    )
                }

                passingLanes.forEach { plotLane ->
                    drawLine(
                        color = colors[plotLane.position % colors.size],
                        start = Offset(30f * (plotLane.position + 1), 0f),
                        end = Offset(30f * (plotLane.position + 1), this.size.height),
                    )
                }
            }
        }

        CommitNode(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = ((itemPosition + 1) * 30 - 15).dp),
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
        modifier = modifier.size(30.dp).border(2.dp, color, shape = CircleShape).clip(CircleShape)
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
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            clipRect {

                if (hasPreviousCommits) drawLine(
                    color = colors[0],
                    start = Offset(30f, this.center.y),
                    end = Offset(30f, this.size.height),
                )

                drawCircle(
                    color = colors[0],
                    radius = 15f,
                    center = Offset(30f, this.center.y),
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
                tint = MaterialTheme.colors.primary,
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
        modifier = Modifier.padding(horizontal = 4.dp).clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = color, shape = RoundedCornerShape(16.dp))
            .combinedClickable(onDoubleClick = onCheckoutRef, onClick = {})
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
                        modifier = Modifier.padding(6.dp).size(14.dp),
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colors.inversePrimaryTextColor,
                    )
                }
                Text(
                    text = ref.simpleLogName,
                    color = MaterialTheme.colors.primaryTextColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                endingContent()
            }
        }
    }
}