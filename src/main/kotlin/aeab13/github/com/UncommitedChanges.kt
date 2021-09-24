package aeab13.github.com

import aeab13.github.com.extensions.filePath
import aeab13.github.com.extensions.icon
import aeab13.github.com.git.StageStatus
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.eclipse.jgit.diff.DiffEntry

@Composable
fun UncommitedChanges(
    gitManager: GitManager,
    onDiffEntrySelected: (DiffEntry) -> Unit,
) {
    val stageStatusState = gitManager.stageStatus.collectAsState()
    val stageStatus = stageStatusState.value

    val (staged, unstaged) = if (stageStatus is StageStatus.Loaded) {
        stageStatus.staged to stageStatus.unstaged
    } else {
        listOf<DiffEntry>() to listOf<DiffEntry>() // return 2 empty lists if still loading
    }

    var commitMessage by remember { mutableStateOf("") }

    Column {
        if (stageStatus is StageStatus.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Text("Staged", fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.weight(5f)) {
            itemsIndexed(staged) { index, diffEntry ->
                FileEntry(
                    diffEntry = diffEntry,
                    icon = Icons.Default.Close,
                    onClick = {
                        onDiffEntrySelected(diffEntry)
                    },
                    onButtonClick = {
                        gitManager.unstage(diffEntry)
                    }
                )

                if (index < staged.size - 1) {
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        Divider(modifier = Modifier.fillMaxWidth())

        Text("Unstaged", fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.weight(5f)) {
            itemsIndexed(unstaged) { index, diffEntry ->
                FileEntry(
                    diffEntry = diffEntry,
                    icon = Icons.Default.Add,
                    onClick = {
                      onDiffEntrySelected(diffEntry)
                    },
                    onButtonClick = {
                        gitManager.stage(diffEntry)
                    }
                )

                if (index < unstaged.size - 1) {
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Column(
            modifier = Modifier
                .height(192.dp)
                .fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true),
                value = commitMessage,
                onValueChange = { commitMessage = it }
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth(),
                onClick = {
                    gitManager.commit(commitMessage)
                },
                enabled = commitMessage.isNotEmpty()
            ) {
                Text("Commit")
            }
        }
    }
}

@Composable
private fun FileEntry(diffEntry: DiffEntry, icon: ImageVector, onClick: () -> Unit, onButtonClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            imageVector = diffEntry.icon,
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(24.dp),
            tint = MaterialTheme.colors.primary,
        )

        Text(
            text = diffEntry.filePath,
            modifier = Modifier.weight(1f, fill = true),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            onClick = onButtonClick,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(32.dp)
                .border(1.dp, MaterialTheme.colors.primary, RoundedCornerShape(10.dp))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}