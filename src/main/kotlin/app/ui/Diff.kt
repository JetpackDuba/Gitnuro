package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
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
import app.git.diff.Line
import app.theme.primaryTextColor
import app.ui.components.ScrollableLazyColumn

@Composable
fun Diff(gitManager: GitManager, diffEntryType: DiffEntryType, onCloseDiffView: () -> Unit) {
    var text by remember { mutableStateOf(listOf<Hunk>()) }

    LaunchedEffect(diffEntryType.diffEntry) {
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
        SelectionContainer {
            ScrollableLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(text) { hunk ->
                    Text(
                        text = hunk.header,
                        color = MaterialTheme.colors.primaryTextColor,
                        modifier = Modifier
                            .background(MaterialTheme.colors.surface)
                            .fillMaxWidth(),
                    )

                    Column {
                        hunk.lines.forEach { line ->
                            val backgroundColor = when (line) {
                                is Line.AddedLine -> {
                                    Color(0x77a9d49b)
                                }
                                is Line.RemovedLine -> {
                                    Color(0x77dea2a2)
                                }
                                is  Line.ContextLine -> {
                                    MaterialTheme.colors.background
                                }
                            }

                            Text(
                                text = line.content,
                                modifier = Modifier
                                    .background(backgroundColor)
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

