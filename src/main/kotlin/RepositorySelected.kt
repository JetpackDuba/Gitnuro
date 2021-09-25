import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import extensions.filePath
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import theme.primaryTextColor
import java.io.IOException

@Composable
fun RepositorySelected(gitManager: GitManager, repository: Repository) {
    var selectedRevCommit by remember {
        mutableStateOf<Pair<RevCommit, List<DiffEntry>>?>(null)
    }

    var diffSelected by remember {
        mutableStateOf<DiffEntry?>(null)
    }
    var uncommitedChangesSelected by remember {
        mutableStateOf<Boolean>(false)
    }

    val selectedIndexCommitLog = remember { mutableStateOf(-1) }

    Row {
        Box(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        ) {
            Branches(gitManager = gitManager)
        }
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
        ) {
            Crossfade(targetState = diffSelected) { diffEntry ->
                when (diffEntry) {
                    null -> {
                        Log(
                            gitManager = gitManager,
                            selectedIndex = selectedIndexCommitLog,
                            onRevCommitSelected = { commit ->
                                uncommitedChangesSelected = false

                                val parent = if (commit.parentCount == 0) {
                                    null
                                } else
                                    commit.parents.first()

                                val oldTreeParser =
                                    prepareTreeParser(repository, parent!!) //TODO Will crash with first commit
                                val newTreeParser = prepareTreeParser(repository, commit)
                                Git(repository).use { git ->
                                    val diffs = git.diff()
                                        .setNewTree(newTreeParser)
                                        .setOldTree(oldTreeParser)
                                        .call()

                                    selectedRevCommit = commit to diffs
                                }
                            },
                            onUncommitedChangesSelected = {
                                uncommitedChangesSelected = true
                                gitManager.updateStatus()
                            }
                        )
                    }
                    else -> {
                        DiffView(
                            gitManager = gitManager,
                            diffEntry = diffEntry,
                            onCloseDiffView = { diffSelected = null })
                    }
                }
            }

        }
        Box(
            modifier = Modifier
                .weight(0.15f)
                .fillMaxHeight()
        ) {
            if (uncommitedChangesSelected) {
                UncommitedChanges(
                    gitManager = gitManager,
                    onDiffEntrySelected = { diffEntry ->
                        println(diffEntry.filePath)
                        diffSelected = diffEntry
                    }
                )
            } else {
                selectedRevCommit?.let {
                    CommitChanges(
                        commitDiff = it,
                        onDiffSelected = { diffEntry ->
                            diffSelected = diffEntry
                        }
                    )
                }
            }
        }
    }
}



@Composable
fun DiffView(gitManager: GitManager, diffEntry: DiffEntry, onCloseDiffView: () -> Unit) {
    val text = remember(diffEntry) {
        gitManager.diffFormat(diffEntry)
    }

    Column {
        Button(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .align(Alignment.End),
            onClick = onCloseDiffView,
        ) {
            Text("Close")
        }
        val textLines = text.split("\n", "\r\n")
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(textLines) { line ->
                val color = if (line.startsWith("+")) {
                    Color(0xFF094f00)
                } else if (line.startsWith("-")) {
                    Color(0xFF4f0000)
                } else {
                    MaterialTheme.colors.primaryTextColor
                }
                SelectionContainer {
                    Text(
                        text = line,
                        color = color,
                        maxLines = 1,
                        fontFamily = FontFamily.Monospace,
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