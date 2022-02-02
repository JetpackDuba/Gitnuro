@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package app.ui.log

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.*
import app.git.graph.GraphNode
import app.theme.*
import app.ui.SelectedItem
import app.ui.components.AvatarImage
import app.ui.components.ScrollableLazyColumn
import app.ui.context_menu.branchContextMenuItems
import app.ui.context_menu.tagContextMenuItems
import app.ui.dialogs.*
import app.viewmodels.LogStatus
import app.viewmodels.LogViewModel
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

// TODO Min size for message column
// TODO Horizontal scroll for the graph
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun Log(
    logViewModel: LogViewModel,
    selectedItem: SelectedItem,
    onItemSelected: (SelectedItem) -> Unit,
    repositoryState: RepositoryState,
) {
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
        val scrollState = rememberLazyListState()

        LaunchedEffect(selectedCommit) {
            // Scroll to commit if a Ref is selected
            if (selectedItem is SelectedItem.Ref) {
                val index = commitList.indexOfFirst { it.name == selectedCommit?.name }
                // TODO Show a message informing the user why we aren't scrolling
                // Index can be -1 if the ref points to a commit that is not shown in the graph due to the limited
                // number of displayed commits.
                if (index >= 0)
                    scrollState.scrollToItem(index)
            }
        }

        LogDialogs(
            logViewModel,
            currentBranch = logStatus.currentBranch,
            onResetShowLogDialog = { showLogDialog.value = LogDialog.None },
            showLogDialog = showLogDialog.value,
        )

        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .fillMaxSize()
        ) {
//            val hasUncommitedChanges by tabViewModel.hasUncommitedChanges.collectAsState()
            val weightMod = remember { mutableStateOf(0f) }
            var graphWidth = (CANVAS_MIN_WIDTH + weightMod.value).dp

            if (graphWidth.value < CANVAS_MIN_WIDTH)
                graphWidth = CANVAS_MIN_WIDTH.dp

            GraphHeader(
                graphWidth = graphWidth,
                weightMod = weightMod,
            )
            ScrollableLazyColumn(
                state = scrollState,
                modifier = Modifier
                    .background(MaterialTheme.colors.background)
                    .fillMaxSize(),
            ) {
                //TODO: Shouldn't this be an item of the graph?
                if (hasUncommitedChanges)
                    item {
                        UncommitedChangesLine(
                            selected = selectedItem == SelectedItem.UncommitedChanges,
                            hasPreviousCommits = commitList.isNotEmpty(),
                            graphWidth = graphWidth,
                            weightMod = weightMod,
                            repositoryState = repositoryState,
                            onUncommitedChangesSelected = {
                                onItemSelected(SelectedItem.UncommitedChanges)
                            }
                        )
                    }
                items(items = commitList) { graphNode ->
                    CommitLine(
                        logViewModel = logViewModel,
                        graphNode = graphNode,
                        selected = selectedCommit?.name == graphNode.name,
                        weightMod = weightMod,
                        graphWidth = graphWidth,
                        currentBranch = logStatus.currentBranch,
                        showCreateNewBranch = { showLogDialog.value = LogDialog.NewBranch(graphNode) },
                        showCreateNewTag = { showLogDialog.value = LogDialog.NewTag(graphNode) },
                        resetBranch = { showLogDialog.value = LogDialog.ResetBranch(graphNode) },
                        onMergeBranch = { ref -> showLogDialog.value = LogDialog.MergeBranch(ref) },
                        onRebaseBranch = { ref -> showLogDialog.value = LogDialog.RebaseBranch(ref) },
                        onRevCommitSelected = {
                            onItemSelected(SelectedItem.Commit(graphNode))
                        }
                    )
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
            NewBranchDialog(
                onReject = onResetShowLogDialog,
                onAccept = { branchName ->
                    logViewModel.createBranchOnCommit(branchName, showLogDialog.graphNode)
                    onResetShowLogDialog()
                }
            )
        }
        is LogDialog.NewTag -> {
            NewTagDialog(
                onReject = onResetShowLogDialog,
                onAccept = { tagName ->
                    logViewModel.createTagOnCommit(tagName, showLogDialog.graphNode)
                    onResetShowLogDialog()
                }
            )
        }
        is LogDialog.MergeBranch -> {
            if (currentBranch != null)
                MergeDialog(
                    currentBranchName = currentBranch.simpleName,
                    mergeBranchName = showLogDialog.ref.simpleName,
                    onReject = onResetShowLogDialog,
                    onAccept = { ff ->
                        logViewModel.mergeBranch(showLogDialog.ref, ff)
                        onResetShowLogDialog()
                    }
                )
        }
        is LogDialog.ResetBranch -> ResetBranchDialog(
            onReject = onResetShowLogDialog,
            onAccept = { resetType ->
                logViewModel.resetToCommit(showLogDialog.graphNode, resetType)
                onResetShowLogDialog()
            }
        )
        is LogDialog.RebaseBranch -> {
            if (currentBranch != null) {
                RebaseDialog(
                    currentBranchName = currentBranch.simpleName,
                    rebaseBranchName = showLogDialog.ref.simpleName,
                    onReject = onResetShowLogDialog,
                    onAccept = {
                        logViewModel.rebaseBranch(showLogDialog.ref)
                        onResetShowLogDialog()
                    }
                )
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(MaterialTheme.colors.graphHeaderBackground),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .width(graphWidth)
                .padding(start = 8.dp),
            text = "Graph",
            color = MaterialTheme.colors.headerText,
            fontSize = 14.sp,
            maxLines = 1,
        )

        DividerLog(
            modifier = Modifier.draggable(rememberDraggableState {
                weightMod.value += it
            }, Orientation.Horizontal)
        )

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .width(graphWidth),
            text = "Message",
            color = MaterialTheme.colors.headerText,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
}

@Composable
fun UncommitedChangesLine(
    selected: Boolean,
    hasPreviousCommits: Boolean,
    graphWidth: Dp,
    weightMod: MutableState<Float>,
    onUncommitedChangesSelected: () -> Unit,
    repositoryState: RepositoryState
) {
    val textColor = if (selected) {
        MaterialTheme.colors.primary
    } else
        MaterialTheme.colors.primaryTextColor

    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .clickable {
                onUncommitedChangesSelected()
            },
    ) {
        UncommitedChangesGraphLine(
            modifier = Modifier
                .width(graphWidth),
            hasPreviousCommits = hasPreviousCommits,
        )

        DividerLog(
            modifier = Modifier
                .draggable(
                    rememberDraggableState {
                        weightMod.value += it
                    },
                    Orientation.Horizontal
                )
        )

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(2f))

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
                color = textColor,
            )

            Spacer(modifier = Modifier.weight(2f))
        }
    }
}

@Composable
fun CommitLine(
    logViewModel: LogViewModel,
    graphNode: GraphNode,
    selected: Boolean,
    weightMod: MutableState<Float>,
    graphWidth: Dp,
    currentBranch: Ref?,
    showCreateNewBranch: () -> Unit,
    showCreateNewTag: () -> Unit,
    resetBranch: (GraphNode) -> Unit,
    onMergeBranch: (Ref) -> Unit,
    onRebaseBranch: (Ref) -> Unit,
    onRevCommitSelected: (GraphNode) -> Unit,
) {
    val commitRefs = graphNode.refs

    Box(modifier = Modifier
        .clickable {
            onRevCommitSelected(graphNode)
        }
    ) {
        ContextMenuArea(
            items = {
                listOf(
                    ContextMenuItem(
                        label = "Checkout commit",
                        onClick = { logViewModel.checkoutCommit(graphNode) }),
                    ContextMenuItem(
                        label = "Create branch",
                        onClick = showCreateNewBranch
                    ),
                    ContextMenuItem(
                        label = "Create tag",
                        onClick = showCreateNewTag
                    ),
                    ContextMenuItem(
                        label = "Revert commit",
                        onClick = { logViewModel.revertCommit(graphNode) }
                    ),

                    ContextMenuItem(
                        label = "Reset current branch to this commit",
                        onClick = { resetBranch(graphNode) }
                    )
                )
            },
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth(),
            ) {
                val nodeColor = colors[graphNode.lane.position % colors.size]

                CommitsGraphLine(
                    modifier = Modifier
                        .width(graphWidth)
                        .fillMaxHeight(),
                    plotCommit = graphNode,
                    nodeColor = nodeColor,
                )

                DividerLog(
                    modifier = Modifier
                        .draggable(
                            rememberDraggableState {
                                weightMod.value += it
                            },
                            Orientation.Horizontal
                        )
                )

                CommitMessage(
                    modifier = Modifier.weight(1f),
                    commit = graphNode,
                    selected = selected,
                    refs = commitRefs,
                    nodeColor = nodeColor,
                    currentBranch = currentBranch,
                    onCheckoutRef = { ref -> logViewModel.checkoutRef(ref) },
                    onMergeBranch = { ref -> onMergeBranch(ref) },
                    onDeleteBranch = { ref -> logViewModel.deleteBranch(ref) },
                    onDeleteTag = { ref -> logViewModel.deleteTag(ref) },
                    onRebaseBranch = { ref -> onRebaseBranch(ref) },
                )
            }
        }
    }
}

@Composable
fun CommitMessage(
    modifier: Modifier = Modifier,
    commit: RevCommit,
    selected: Boolean,
    refs: List<Ref>,
    currentBranch: Ref?,
    nodeColor: Color,
    onCheckoutRef: (ref: Ref) -> Unit,
    onMergeBranch: (ref: Ref) -> Unit,
    onDeleteBranch: (ref: Ref) -> Unit,
    onRebaseBranch: (ref: Ref) -> Unit,
    onDeleteTag: (ref: Ref) -> Unit,
) {
    val textColor = if (selected) {
        MaterialTheme.colors.primary
    } else
        MaterialTheme.colors.primaryTextColor

    val secondaryTextColor = if (selected) {
        MaterialTheme.colors.primary
    } else
        MaterialTheme.colors.secondaryTextColor

    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.weight(2f))
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            refs
                .sortedWith { ref1, ref2 ->
                    if (ref1.isSameBranch(currentBranch)) {
                        -1
                    } else {
                        ref1.name.compareTo(ref2.name)
                    }
                }
                .forEach { ref ->
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
                            isCurrentBranch = ref.isSameBranch(currentBranch),
                            onCheckoutBranch = { onCheckoutRef(ref) },
                            onMergeBranch = { onMergeBranch(ref) },
                            onDeleteBranch = { onDeleteBranch(ref) },
                            onRebaseBranch = { onRebaseBranch(ref) },
                        )
                    }
                }

            Text(
                text = commit.shortMessage,
                modifier = Modifier.padding(start = 16.dp),
                fontSize = 14.sp,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.weight(2f))

            Text(
                text = commit.committerIdent.`when`.toSmartSystemString(),
                modifier = Modifier.padding(horizontal = 16.dp),
                fontSize = 12.sp,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

        }
        Spacer(modifier = Modifier.weight(2f))
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DividerLog(modifier: Modifier) {
    Box(
        modifier = Modifier
            .width(8.dp)
            .then(modifier)
            .pointerHoverIcon(PointerIconDefaults.Hand)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(color = MaterialTheme.colors.primary)
                .align(Alignment.Center)
        )
    }
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
            modifier = Modifier
                .fillMaxSize()
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
fun UncommitedChangesGraphLine(
    modifier: Modifier = Modifier,
    hasPreviousCommits: Boolean,
) {
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            clipRect {

                if (hasPreviousCommits)
                    drawLine(
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
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    color: Color,
) {
    val contextMenuItemsList = {
        branchContextMenuItems(
            isCurrentBranch = isCurrentBranch,
            isLocal = ref.isLocal,
            onCheckoutBranch = onCheckoutBranch,
            onMergeBranch = onMergeBranch,
            onDeleteBranch = onDeleteBranch,
            onRebaseBranch = onRebaseBranch,
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
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, color = color, shape = RoundedCornerShape(16.dp))
            .combinedClickable(
                onDoubleClick = onCheckoutRef,
                onClick = {}
            )
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
                        tint = MaterialTheme.colors.inversePrimaryTextColor,
                    )
                }
                Text(
                    text = ref.simpleVisibleName,
                    color = MaterialTheme.colors.primaryTextColor,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                )

                endingContent()
            }
        }
    }
}