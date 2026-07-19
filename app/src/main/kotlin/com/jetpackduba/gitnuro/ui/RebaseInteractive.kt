package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.RebaseLine
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.extensions.backgroundIf
import com.jetpackduba.gitnuro.repositoryopen.RepositoryOpenViewModel
import com.jetpackduba.gitnuro.theme.backgroundSelected
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.drag_sorting.VerticalDraggableItem
import com.jetpackduba.gitnuro.ui.drag_sorting.rememberVerticalDragDropState
import com.jetpackduba.gitnuro.ui.drag_sorting.verticalDragContainer
import com.jetpackduba.gitnuro.viewmodels.RebaseAction
import com.jetpackduba.gitnuro.viewmodels.RebaseInteractiveViewState
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun RebaseInteractive(
    viewModel: RepositoryOpenViewModel,
    state: RebaseInteractiveViewState,
) {
    val selectedItem = viewModel.selectedItem.collectAsState().value

    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize(),
    ) {
        when (state) {
            is RebaseInteractiveViewState.None, is RebaseInteractiveViewState.Failed -> {}
            is RebaseInteractiveViewState.Loaded -> {
                RebaseStateLoaded(
                    viewModel,
                    state,
                    selectedItem,
                    onFocusLine = {
                        if (
                            selectedItem !is SelectedItem.CommitItem ||
                            !selectedItem.commit.hash.startsWith(it.commit)
                        ) {
                            viewModel.selectLine(it)
                        }
                    },
                    onCancel = {
                        viewModel.cancel()
                    },
                    onMoveCommit = { from, to ->
                        viewModel.moveCommit(from, to)
                    }
                )
            }

            RebaseInteractiveViewState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RebaseStateLoaded(
    viewModel: RepositoryOpenViewModel,
    rebaseState: RebaseInteractiveViewState.Loaded,
    selectedItem: SelectedItem,
    onFocusLine: (RebaseLine) -> Unit,
    onCancel: () -> Unit,
    onMoveCommit: (from: Int, to: Int) -> Unit,
) {
    val stepsList = rebaseState.stepsList

    Column(
        modifier = Modifier.fillMaxSize()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.background)
    ) {
        Text(
            text = stringResource(Res.string.rebase_interactive_view_title),
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
            fontSize = 20.sp,
        )

        val listState = rememberLazyListState()
        val state = rememberVerticalDragDropState(listState) { fromIndex, toIndex ->
            println("P0: $fromIndex\nP1: $toIndex")
            onMoveCommit(fromIndex, toIndex)
        }

        ScrollableLazyColumn(
            modifier = Modifier
                .weight(1f)
                .verticalDragContainer(state, onDraggedItem = {
                    println("OnDragItem $it")
                }),
            state = listState,
        ) {
            itemsIndexed(
                stepsList,
                key = { _, line -> line.commit },
            ) { index, rebaseTodoLine ->
                VerticalDraggableItem(state, index) {
                    RebaseCommit(
                        rebaseLine = rebaseTodoLine,
                        isSelected = selectedItem is SelectedItem.CommitItem && selectedItem.commit.hash.startsWith(
                            rebaseTodoLine.commit
                        ),
                        isFirst = stepsList.first() == rebaseTodoLine,
                        onFocusLine = { onFocusLine(rebaseTodoLine) },
                        onActionChanged = { newAction ->
                            viewModel.onCommitActionChanged(rebaseTodoLine.commit, newAction)
                        },
                        onMessageChanged = { newMessage ->
                            viewModel.onCommitMessageChanged(rebaseTodoLine, newMessage)
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(Res.string.generic_button_cancel),
                modifier = Modifier.padding(end = 8.dp),
                onClick = onCancel,
                backgroundColor = Color.Transparent,
                textColor = MaterialTheme.colors.onBackground,
            )

            PrimaryButton(
                modifier = Modifier.padding(end = 16.dp),
                enabled = true, // TODO Moving commits may also affect stepsList.any { it.rebaseAction != RebaseAction.PICK },
                onClick = {
                    viewModel.continueRebaseInteractive()
                },
                text = stringResource(Res.string.rebase_interactive_view_button_complete_rebase)
            )
        }
    }
}

@Composable
fun RebaseCommit(
    rebaseLine: RebaseLine,
    isFirst: Boolean,
    isSelected: Boolean,
    onFocusLine: () -> Unit,
    onActionChanged: (RebaseLine.Action) -> Unit,
    onMessageChanged: (String) -> Unit,
) {
    val action = rebaseLine.action
    val focusRequester = remember { FocusRequester() }


    var newMessage by remember(rebaseLine.commit, action) {
        val message = if (rebaseLine.action == RebaseLine.Action.REWORD && rebaseLine.modifiedMessage != null) {
            rebaseLine.modifiedMessage.orEmpty()
        } else {
            rebaseLine.fullMessage
        }

        mutableStateOf(message)
    }

    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .clickable {
                onFocusLine()
            }
            .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionDropdown(
            action,
            isFirst = isFirst,
            onActionDropDownClicked = onFocusLine,
            onActionChanged = { onActionChanged(it) },
        )

        AdjustableOutlinedTextField(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
                .heightIn(min = 40.dp)
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.hasFocus && !isSelected) {
                        onFocusLine()
                    }
                },
            enabled = action == RebaseLine.Action.REWORD,
            value = newMessage,
            onValueChange = {
                newMessage = it
                onMessageChanged(it)
            },
            textStyle = MaterialTheme.typography.body2,
            backgroundColor = if (action == RebaseLine.Action.REWORD) {
                MaterialTheme.colors.background
            } else
                MaterialTheme.colors.surface
        )
    }
}


@Composable
fun ActionDropdown(
    action: RebaseLine.Action,
    isFirst: Boolean,
    onActionDropDownClicked: () -> Unit,
    onActionChanged: (RebaseLine.Action) -> Unit,
) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .border(
                width = 2.dp,
                color = MaterialTheme.colors.onBackgroundSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
            )
    ) {
        TextButton(
            onClick = {
                showDropDownMenu = true
                onActionDropDownClicked()
            },
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
        ) {
            Text(
                getActionDisplayName(action),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.weight(1f)
            )

            Icon(
                painterResource(Res.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp),
                tint = MaterialTheme.colors.onBackground,
            )
        }

        DropdownMenu(
            expanded = showDropDownMenu,
            onDismissRequest = { showDropDownMenu = false },
        ) {
            val dropDownItems = if (isFirst) {
                firstItemActions
            } else {
                actions
            }

            for (dropDownOption in dropDownItems) {
                DropdownMenuItem(
                    onClick = {
                        showDropDownMenu = false
                        onActionChanged(dropDownOption.value)
                    }
                ) {
                    Text(
                        text = dropDownOption.displayName,
                        style = MaterialTheme.typography.body1,
                    )
                }
            }
        }
    }
}

fun getActionDisplayName(action: RebaseLine.Action): String {
    return when (action) {
        RebaseLine.Action.PICK -> "Pick"
        RebaseLine.Action.REWORD -> "Reword"
        RebaseLine.Action.SQUASH -> "Squash"
        RebaseLine.Action.FIXUP -> "Fixup"
        RebaseLine.Action.EDIT -> "Edit"
        RebaseLine.Action.DROP -> "Drop"
        RebaseLine.Action.COMMENT -> "Comment"
    }
}

val firstItemActions = listOf(
    RebaseAction.PICK,
    RebaseAction.REWORD,
    RebaseAction.DROP,
)

val actions = listOf(
    RebaseAction.PICK,
    RebaseAction.REWORD,
    RebaseAction.SQUASH,
    RebaseAction.FIXUP,
    RebaseAction.EDIT,
    RebaseAction.DROP,
)