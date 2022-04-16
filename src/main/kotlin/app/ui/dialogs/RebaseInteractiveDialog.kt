package app.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.theme.primaryTextColor
import app.ui.components.PrimaryButton
import app.ui.components.ScrollableLazyColumn
import app.viewmodels.RebaseInteractiveState
import app.viewmodels.RebaseInteractiveViewModel
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.RebaseTodoLine.Action
import org.eclipse.jgit.revwalk.RevCommit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RebaseInteractiveDialog(
    rebaseInteractiveViewModel: RebaseInteractiveViewModel,
    revCommit: RevCommit,
    onClose: () -> Unit,
) {
    val rebaseState = rebaseInteractiveViewModel.rebaseState.collectAsState()
    val rebaseStateValue = rebaseState.value

    LaunchedEffect(Unit) {
        rebaseInteractiveViewModel.revCommit = revCommit
        rebaseInteractiveViewModel.startRebaseInteractive()
    }

    MaterialDialog {

        Box(
            modifier = Modifier
                .background(MaterialTheme.colors.background)
                .fillMaxSize(0.8f),
        ) {
            when (rebaseStateValue) {
                is RebaseInteractiveState.Failed -> {}
                RebaseInteractiveState.Finished -> onClose()
                is RebaseInteractiveState.Loaded -> {
                    RebaseStateLoaded(
                        rebaseInteractiveViewModel,
                        rebaseStateValue,
                        onCancel = onClose,
                    )
                }
                RebaseInteractiveState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
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
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Rebase interactive",
            color = MaterialTheme.colors.primaryTextColor,
            modifier = Modifier.padding(all = 16.dp)
        )

        ScrollableLazyColumn(modifier = Modifier.weight(1f)) {
            items(rebaseState.stepsList) { rebaseTodoLine ->
                RebaseCommit(
                    rebaseLine = rebaseTodoLine,
                    message = rebaseState.messages[rebaseTodoLine.commit.name()],
                    onActionChanged = { newAction ->
                        rebaseInteractiveViewModel.onCommitActionChanged(rebaseTodoLine.commit, newAction)
                    },
                    onMessageChanged = { newMessage ->
                        rebaseInteractiveViewModel.onCommitMessageChanged(rebaseTodoLine.commit, newMessage)
                    },
                )
            }
        }

        Row {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    onCancel()
                }
            ) {
                Text("Cancel")
            }
            PrimaryButton(
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
    message: String?,
    onActionChanged: (Action) -> Unit,
    onMessageChanged: (String) -> Unit,
) {
    val action = rebaseLine.action
    var newMessage by remember(rebaseLine.commit.name(), action) {
        if(action == Action.REWORD) {
            mutableStateOf(message ?: rebaseLine.shortMessage) /* if reword, use the value from the map (if possible)*/ } else
            mutableStateOf(rebaseLine.shortMessage) // If it's not reword, use the original shortMessage

    }

    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ActionDropdown(
            rebaseLine.action,
            onActionChanged = onActionChanged,
        )

        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp),
            enabled = rebaseLine.action == Action.REWORD,
            value = newMessage,
            onValueChange = {
                newMessage = it
                onMessageChanged(it)
            },
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.background),
            textStyle = TextStyle.Default.copy(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
        )

    }
}


@Composable
fun ActionDropdown(
    action: Action,
    onActionChanged: (Action) -> Unit,
) {
    var showDropDownMenu by remember { mutableStateOf(false) }
    Box {
        PrimaryButton(
            onClick = { showDropDownMenu = true },
            modifier = Modifier
                .width(120.dp)
                .height(48.dp)
                .padding(end = 8.dp),
            text = action.toToken()
        )
        DropdownMenu(
            expanded = showDropDownMenu,
            onDismissRequest = { showDropDownMenu = false },
        ) {
            for (dropDownOption in actions) {
                DropdownMenuItem(
                    onClick = {
                        showDropDownMenu = false
                        onActionChanged(dropDownOption)
                    }
                ) {
                    Text(dropDownOption.toToken())
                }
            }
        }
    }
}

val actions = listOf(
    Action.PICK,
    Action.REWORD,
    Action.SQUASH,
    Action.FIXUP,
)


