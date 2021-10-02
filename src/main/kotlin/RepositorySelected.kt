import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import credentials.CredentialsState
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.IOException


@ExperimentalMaterialApi
@Composable
fun RepositorySelected(gitManager: GitManager, repository: Repository) {
    var selectedRevCommit by remember {
        mutableStateOf<Pair<RevCommit, List<DiffEntry>>?>(null)
    }

    var diffSelected by remember {
        mutableStateOf<DiffEntryType?>(null)
    }
    var uncommitedChangesSelected by remember {
        mutableStateOf<Boolean>(false)
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
            Column (
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
                Button(onClick = {gitManager.credentialsAccepted(userField, passwordField)}) {
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

                                val parent = if (commit.parentCount == 0) {
                                    null
                                } else
                                    commit.parents.first()

                                val oldTreeParser = if (parent != null)
                                    prepareTreeParser(repository, parent)
                                else {
                                    CanonicalTreeParser()
                                }

                                val newTreeParser = prepareTreeParser(repository, commit)

                                Git(repository).use { git ->
                                    val diffs = git.diff()
                                        .setNewTree(newTreeParser)
                                        .setOldTree(oldTreeParser)
                                        .call()

                                    selectedRevCommit = commit to diffs
                                }


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
                        commitDiff = it,
                        onDiffSelected = { diffEntry ->
                            diffSelected = DiffEntryType.CommitDiff(diffEntry)
                        }
                    )
                }
            }
        }
    }
}


@Throws(IOException::class)
fun prepareTreeParser(repository: Repository, commit: RevCommit): AbstractTreeIterator? {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevWalk(repository).use { walk ->
        val tree: RevTree = walk.parseTree(commit.tree.id)
        val treeParser = CanonicalTreeParser()
        repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }
        walk.dispose()
        return treeParser
    }
}