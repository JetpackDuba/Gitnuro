package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.git.DiffEntryType
import app.git.GitManager
import app.git.diff.Hunk
import app.git.diff.LineType
import app.theme.primaryTextColor
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SecondaryButton
import org.eclipse.jgit.diff.DiffEntry

@Composable
fun Diff(gitManager: GitManager, diffEntryType: DiffEntryType, onCloseDiffView: () -> Unit) {
    var text by remember { mutableStateOf(listOf<Hunk>()) }

    LaunchedEffect(Unit) {
        text = gitManager.diffFormat(diffEntryType)


        if (text.isEmpty()) onCloseDiffView()
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        OutlinedButton(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .align(Alignment.End),
            onClick = onCloseDiffView,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.primary,
            )
        ) {
            Text("Close diff")
        }

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
//                .padding(16.dp)
        ) {
            itemsIndexed(text) { index, hunk ->
                val hunksSeparation = if (index == 0)
                    0.dp
                else
                    16.dp
                Row(
                    modifier = Modifier
                        .padding(top = hunksSeparation)
                        .background(MaterialTheme.colors.surface)
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = hunk.header,
                        color = MaterialTheme.colors.primaryTextColor,
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    if (
                        (diffEntryType is DiffEntryType.StagedDiff || diffEntryType is DiffEntryType.UnstagedDiff) &&
                        diffEntryType.diffEntry.changeType == DiffEntry.ChangeType.MODIFY
                    ) {
                        val buttonText: String
                        val color: Color
                        if (diffEntryType is DiffEntryType.StagedDiff) {
                            buttonText = "Unstage"
                            color = MaterialTheme.colors.error
                        } else {
                            buttonText = "Stage"
                            color = MaterialTheme.colors.primary
                        }

                        SecondaryButton(
                            text = buttonText,
                            backgroundButton = color,
                            onClick = {
                                if (diffEntryType is DiffEntryType.StagedDiff) {
                                    gitManager.unstageHunk(diffEntryType.diffEntry, hunk)
                                } else {
                                    gitManager.stageHunk(diffEntryType.diffEntry, hunk)
                                }
                            }
                        )
                    }
                }

                SelectionContainer {
                    Column {
                        hunk.lines.forEach { line ->
                            val backgroundColor = when (line.lineType) {
                                LineType.ADDED -> {
                                    Color(0x77a9d49b)
                                }
                                LineType.REMOVED -> {
                                    Color(0x77dea2a2)
                                }
                                LineType.CONTEXT -> {
                                    MaterialTheme.colors.background
                                }
                            }

                            Text(
                                text = line.text,
                                modifier = Modifier
                                    .background(backgroundColor)
                                    .padding(start = 16.dp)
                                    .fillMaxWidth(),
                                color = MaterialTheme.colors.primaryTextColor,
                                maxLines = 1,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

