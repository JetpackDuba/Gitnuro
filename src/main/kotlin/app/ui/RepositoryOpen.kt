package app.ui

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
import app.credentials.CredentialsState
import app.git.DiffEntryType
import app.git.GitManager
import app.git.dialogs.NewBranchDialog
import app.git.dialogs.UserPasswordDialog
import openRepositoryDialog
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

    var showBranchDialog by remember {
        mutableStateOf(false)
    }

    val selectedIndexCommitLog = remember { mutableStateOf(-1) }

    val credentialsState by gitManager.credentialsState.collectAsState()

    if (credentialsState == CredentialsState.CredentialsRequested) {
        UserPasswordDialog(
            onReject = {
                gitManager.credentialsDenied()
            },
            onAccept = { user, password ->
                gitManager.credentialsAccepted(user, password)
            }
        )
    }

    if (showBranchDialog) {
        NewBranchDialog(
            onReject = {
                showBranchDialog = false
            },
            onAccept = { branchName ->
                gitManager.createBranch(branchName)
            }
        )
    }

    Column {
        GMenu(
            onRepositoryOpen = {
                openRepositoryDialog(gitManager = gitManager)
            },
            onPull = { gitManager.pull() },
            onPush = { gitManager.push() },
            onStash = { gitManager.stash() },
            onPopStash = { gitManager.popStash() },
            onCreateBranch = { showBranchDialog = true }
        )

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
}