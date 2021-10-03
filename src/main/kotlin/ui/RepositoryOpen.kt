package ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import credentials.CredentialsState
import git.DiffEntryType
import git.GitManager
import org.eclipse.jgit.revwalk.RevCommit


@Composable
fun RepositoryOpenPage(gitManager: GitManager) {
    var selectedRevCommit by remember {
        mutableStateOf<RevCommit?>(null)
    }

    var diffSelected by remember {
        mutableStateOf<DiffEntryType?>(null)
    }
    var uncommitedChangesSelected by remember {
        mutableStateOf(false)
    }

    val selectedIndexCommitLog = remember { mutableStateOf(-1) }

    val credentialsState by gitManager.credentialsState.collectAsState()

    if (credentialsState == CredentialsState.CredentialsRequested) {
        var userField by remember { mutableStateOf("") }
        var passwordField by remember { mutableStateOf("") }

        Dialog(
            onCloseRequest = {
                gitManager.credentialsDenied()
            },
            title = "",

            ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Introduce your remote server credentials")

                OutlinedTextField(
                    value = userField,
                    label = { Text("User", fontSize = 14.sp) },
                    textStyle = TextStyle(fontSize = 14.sp),
                    onValueChange = {
                        userField = it
                    },
                )
                OutlinedTextField(
                    modifier = Modifier.padding(bottom = 8.dp),
                    value = passwordField,
                    label = { Text("Password", fontSize = 14.sp) },
                    textStyle = TextStyle(fontSize = 14.sp),
                    onValueChange = {
                        passwordField = it
                    },
                    visualTransformation = PasswordVisualTransformation()
                )
                Button(onClick = { gitManager.credentialsAccepted(userField, passwordField) }) {
                    Text("Ok")
                }
            }
        }
    }



    Row {
        Column(
            modifier = Modifier
                .widthIn(min = 300.dp)
                .weight(0.15f)
                .fillMaxHeight()
        ) {
            Branches(gitManager = gitManager)
            Stashes(gitManager = gitManager)
        }
        Box(
            modifier = Modifier
                .weight(0.60f)
                .fillMaxHeight()
        ) {
            Crossfade(targetState = diffSelected) { diffEntry ->
                when (diffEntry) {
                    null -> {
                        Log(
                            gitManager = gitManager,
                            selectedIndex = selectedIndexCommitLog,
                            onRevCommitSelected = { commit ->
                                // TODO Move all this code to tree manager

                                selectedRevCommit = commit
                                uncommitedChangesSelected = false
                            },
                            onUncommitedChangesSelected = {
                                gitManager.statusShouldBeUpdated()
                                uncommitedChangesSelected = true
                            }
                        )
                    }
                    else -> {
                        Diff(
                            gitManager = gitManager,
                            diffEntryType = diffEntry,
                            onCloseDiffView = { diffSelected = null })
                    }
                }
            }

        }
        Box(
            modifier = Modifier
                .weight(0.25f)
                .fillMaxHeight()
        ) {
            if (uncommitedChangesSelected) {
                UncommitedChanges(
                    gitManager = gitManager,
                    onStagedDiffEntrySelected = { diffEntry ->
                        diffSelected = DiffEntryType.StagedDiff(diffEntry)
                    },
                    onUnstagedDiffEntrySelected = { diffEntry ->
                        diffSelected = DiffEntryType.UnstagedDiff(diffEntry)
                    }
                )
            } else {
                selectedRevCommit?.let {
                    CommitChanges(
                        gitManager = gitManager,
                        commit = it,
                        onDiffSelected = { diffEntry ->
                            diffSelected = DiffEntryType.CommitDiff(diffEntry)
                        }
                    )
                }
            }
        }
    }
}

