import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import extensions.filePath
import extensions.icon
import git.StageStatus
import org.eclipse.jgit.diff.DiffEntry
import theme.headerBackground

@OptIn(ExperimentalAnimationApi::class)
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
        AnimatedVisibility(
            visible = stageStatus is StageStatus.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }


        EntriesList(
            modifier = Modifier
                .padding(8.dp)
                .weight(5f)
                .fillMaxWidth(),
            title = "Staged",
            optionIcon = Icons.Default.Close,
            diffEntries = staged,
            onDiffEntrySelected = onDiffEntrySelected,
            onDiffEntryOptionSelected = {
                gitManager.unstage(it)
            }
        )

        EntriesList(
            modifier = Modifier
                .padding(8.dp)
                .weight(5f)
                .fillMaxWidth(),
            title = "Unstaged",
            optionIcon = Icons.Default.Add,
            diffEntries = unstaged,
            onDiffEntrySelected = onDiffEntrySelected,
            onDiffEntryOptionSelected = {
                gitManager.stage(it)
            }
        )

        Card(
            modifier = Modifier
                .padding(8.dp)
                .height(192.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = true),
                    value = commitMessage,
                    onValueChange = { commitMessage = it },
                    label = { Text("Write your commit message here") },
                    colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface)
                )

                Button(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = {
                        gitManager.commit(commitMessage)
                        commitMessage = ""
                    },
                    enabled = commitMessage.isNotEmpty() && staged.isNotEmpty(),
                    shape = RectangleShape,
                ) {
                    Text("Commit")
                }
            }
        }
    }
}

@Composable
private fun EntriesList(
    modifier: Modifier,
    title: String,
    optionIcon: ImageVector,
    diffEntries: List<DiffEntry>,
    onDiffEntrySelected: (DiffEntry) -> Unit,
    onDiffEntryOptionSelected: (DiffEntry) -> Unit,
) {
    Card(
        modifier = modifier
    ) {
        Column {
            Text(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.headerBackground)
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                text = title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                maxLines = 1,
            )

            LazyColumn(modifier = Modifier.weight(5f)) {
                itemsIndexed(diffEntries) { index, diffEntry ->
                    FileEntry(
                        diffEntry = diffEntry,
                        icon = optionIcon,
                        onClick = {
                            onDiffEntrySelected(diffEntry)
                        },
                        onButtonClick = {
                            onDiffEntryOptionSelected(diffEntry)
                        }
                    )

                    if (index < diffEntries.size - 1) {
                        Divider(modifier = Modifier.fillMaxWidth())
                    }
                }
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