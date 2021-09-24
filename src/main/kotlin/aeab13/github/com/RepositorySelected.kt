package aeab13.github.com

import aeab13.github.com.extensions.filePath
import aeab13.github.com.extensions.icon
import aeab13.github.com.git.LogStatus
import aeab13.github.com.theme.backgroundColor
import aeab13.github.com.theme.primaryTextColor
import aeab13.github.com.theme.secondaryTextColor
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.ByteArrayOutputStream
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
                        DiffView(gitManager = gitManager, diffEntry = diffEntry, onCloseDiffView = { diffSelected = null })
                    }
                }
            }

        }
        Box(
            modifier = Modifier
                .weight(0.3f)
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

@Composable
fun CommitChanges(commitDiff: Pair<RevCommit, List<DiffEntry>>, onDiffSelected: (DiffEntry) -> Unit) {
    val commit = commitDiff.first
    val diff = commitDiff.second

    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        Text(commit.fullMessage)
        Text(commit.commitTime.toString())

        CommitLogChanges(diff, onDiffSelected = onDiffSelected)
    }
}

@Composable
fun Log(
    gitManager: GitManager,
    onRevCommitSelected: (RevCommit) -> Unit,
    onUncommitedChangesSelected: () -> Unit,
    selectedIndex: MutableState<Int> = remember { mutableStateOf(-1) }
) {

    val logStatusState = gitManager.logStatus.collectAsState()
    val logStatus = logStatusState.value

    val selectedUncommited = remember { mutableStateOf(false) }

    val log = if (logStatus is LogStatus.Loaded) {
        logStatus.commits
    } else
        listOf()


    LazyColumn(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize()
    ) {
        if (gitManager.hasUncommitedChanges())
            item {
                val textColor = if (selectedUncommited.value) {
                    MaterialTheme.colors.primary
                } else
                    MaterialTheme.colors.primaryTextColor

                Column(
                    modifier = Modifier
                        .height(64.dp)
                        .fillMaxWidth()
                        .clickable {
                            selectedIndex.value = -1
                            selectedUncommited.value = true
                            onUncommitedChangesSelected()
                        },
                    verticalArrangement = Arrangement.Center,
                ) {
                    Spacer(modifier = Modifier.weight(2f))

                    Text(
                        text = "Uncommited changes",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp),
                        color = textColor,
                    )
                    Text(
                        text = "You",
                        modifier = Modifier.padding(start = 16.dp),
                        color = MaterialTheme.colors.secondaryTextColor,
                    )
                    Spacer(modifier = Modifier.weight(2f))

                    Divider()
                }
            }

        itemsIndexed(items = log) { index, item ->
            val textColor = if (selectedIndex.value == index) {
                MaterialTheme.colors.primary
            } else
                MaterialTheme.colors.primaryTextColor

            Column(
                modifier = Modifier
                    .height(64.dp)
                    .fillMaxWidth()
                    .clickable {
                        selectedIndex.value = index
                        selectedUncommited.value = false
                        onRevCommitSelected(item)
                    },
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.weight(2f))

                Text(
                    text = item.shortMessage,
                    modifier = Modifier.padding(start = 16.dp),
                    color = textColor,
                )
                Text(
                    text = item.authorIdent.name,
                    modifier = Modifier.padding(start = 16.dp),
                    color = MaterialTheme.colors.secondaryTextColor,
                )
                Spacer(modifier = Modifier.weight(2f))

                Divider()
            }
        }
    }
}

@Composable
fun CommitLogChanges(diffEntries: List<DiffEntry>, onDiffSelected: (DiffEntry) -> Unit) {
    val selectedIndex = remember(diffEntries) { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxSize()
    ) {
        itemsIndexed(items = diffEntries) { index, diffEntry ->
            val textColor = if (selectedIndex.value == index) {
                MaterialTheme.colors.primary
            } else
                MaterialTheme.colors.primaryTextColor

            Column(
                modifier = Modifier
                    .height(56.dp)
                    .fillMaxWidth()
                    .clickable {
                        selectedIndex.value = index
                        onDiffSelected(diffEntry)
                    },
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.weight(2f))


                Row {
                    Icon(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(24.dp),
                        imageVector = diffEntry.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                    )

                    Text(
                        text = diffEntry.filePath,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.weight(2f))

                Divider()
            }
        }
    }
}
