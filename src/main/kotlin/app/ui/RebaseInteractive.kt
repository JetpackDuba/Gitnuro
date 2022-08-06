package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.AdjustableOutlinedTextField
import app.ui.components.PrimaryButton
import app.ui.components.ScrollableLazyColumn
import app.viewmodels.RebaseInteractiveState
import app.viewmodels.RebaseInteractiveViewModel
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RebaseTodoLine.Action

@Composable
fun RebaseInteractive(
    rebaseInteractiveViewModel: RebaseInteractiveViewModel,
) {
    val rebaseState = rebaseInteractiveViewModel.rebaseState.collectAsState()
    val rebaseStateValue = rebaseState.value

    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize(),
    ) {
        when (rebaseStateValue) {
            is RebaseInteractiveState.Failed -> {}
            is RebaseInteractiveState.Loaded -> {
                RebaseStateLoaded(
                    rebaseInteractiveViewModel,
                    rebaseStateValue,
                    onCancel = {
                        rebaseInteractiveViewModel.cancel()
                    },
                )
            }
            RebaseInteractiveState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun RebaseStateLoaded(
    rebaseInteractiveViewModel: RebaseInteractiveViewModel,
    rebaseState: RebaseInteractiveState.Loaded,
    onCancel: () -> Unit,
) {
    val stepsList = rebaseState.stepsList

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Rebase interactive",
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            fontSize = 20.sp,
        )

        ScrollableLazyColumn(modifier = Modifier.weight(1f)) {
            items(stepsList) { rebaseTodoLine ->
                RebaseCommit(
                    rebaseLine = rebaseTodoLine,
                    message = rebaseState.messages[rebaseTodoLine.commit.name()],
                    isFirst = stepsList.first() == rebaseTodoLine,
                    onActionChanged = { newAction ->
                        rebaseInteractiveViewModel.onCommitActionChanged(rebaseTodoLine.commit, newAction)
                    },
                    onMessageChanged = { newMessage ->
                        rebaseInteractiveViewModel.onCommitMessageChanged(rebaseTodoLine.commit, newMessage)
                    },
                )
            }
        }

        Row(modifier = Modifier.padding(bottom = 16.dp)) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    onCancel()
                },
                colors = textButtonColors(),
            ) {
                Text("Cancel")
            }
            PrimaryButton(
                modifier = Modifier.padding(end = 16.dp),
                enabled = stepsList.any { it.action != Action.PICK },
                onClick = {
                    rebaseInteractiveViewModel.continueRebaseInteractive()
                },
                text = "Complete rebase"
            )
        }
    }
}

@Composable
fun RebaseCommit(
    rebaseLine: RebaseTodoLine,
    isFirst: Boolean,
    message: String?,
    onActionChanged: (Action) -> Unit,
    onMessageChanged: (String) -> Unit,
) {
    val action = rebaseLine.action
    var newMessage by remember(rebaseLine.commit.name(), action) {
        if (action == Action.REWORD) {
            mutableStateOf(message ?: rebaseLine.shortMessage) /* if reword, use the value from the map (if possible)*/
        } else
            mutableStateOf(rebaseLine.shortMessage) // If it's not reword, use the original shortMessage
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min)
    ) {
        ActionDropdown(
            rebaseLine.action,
            isFirst = isFirst,
            onActionChanged = onActionChanged,
        )

        AdjustableOutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .height(40.dp),
            enabled = rebaseLine.action == Action.REWORD,
            value = newMessage,
            onValueChange = {
                newMessage = it
                onMessageChanged(it)
            },
            textStyle = MaterialTheme.typography.body2,
        )

    }
}


@Composable
fun ActionDropdown(
    action: Action,
    isFirst: Boolean,
    onActionChanged: (Action) -> Unit,
) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    Box {
        PrimaryButton(
            onClick = { showDropDownMenu = true },
            modifier = Modifier
                .width(120.dp)
                .height(40.dp)
                .padding(end = 8.dp),
            text = action.toToken().replaceFirstChar { it.uppercase() }
        )

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
                        onActionChanged(dropDownOption)
                    }
                ) {
                    Text(
                        text = dropDownOption.toToken().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.body1,
                    )
                }
            }
        }
    }
}

val firstItemActions = listOf(
    Action.PICK,
    Action.REWORD,
)

val actions = listOf(
    Action.PICK,
    Action.REWORD,
    Action.SQUASH,
    Action.FIXUP,
)


