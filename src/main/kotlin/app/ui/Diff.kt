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
import app.git.diff.LineType
import app.theme.primaryTextColor
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SecondaryButton
import app.viewmodels.DiffViewModel
import org.eclipse.jgit.diff.DiffEntry

@Composable
fun Diff(
    diffViewModel: DiffViewModel,
    onCloseDiffView: () -> Unit,
) {
    val diffResultState = diffViewModel.diffResult.collectAsState()
    val diffResult = diffResultState.value ?: return

    val diffEntryType = diffResult.diffEntryType
    val diffEntry = diffEntryType.diffEntry
    val hunks = diffResult.hunks

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val filePath = if(diffEntry.newPath != "/dev/null")
                diffEntry.newPath
            else
                diffEntry.oldPath

            Text(
                text = filePath,
                color = MaterialTheme.colors.primaryTextColor,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                onClick = onCloseDiffView,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.background,
                    contentColor = MaterialTheme.colors.primary,
                )
            ) {
                Text("Close diff")
            }
        }

        val scrollState by diffViewModel.lazyListState.collectAsState()
        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = scrollState
        ) {
            itemsIndexed(hunks) { index, hunk ->
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
                            buttonText = "Unstage hunk"
                            color = MaterialTheme.colors.error
                        } else {
                            buttonText = "Stage hunk"
                            color = MaterialTheme.colors.primary
                        }

                        SecondaryButton(
                            text = buttonText,
                            backgroundButton = color,
                            onClick = {
                                if (diffEntryType is DiffEntryType.StagedDiff) {
                                    diffViewModel.unstageHunk(diffEntryType.diffEntry, hunk)
                                } else {
                                    diffViewModel.stageHunk(diffEntryType.diffEntry, hunk)
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

