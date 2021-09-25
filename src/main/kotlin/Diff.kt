import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.eclipse.jgit.diff.DiffEntry
import theme.primaryTextColor

@Composable
fun Diff(gitManager: GitManager, diffEntry: DiffEntry, onCloseDiffView: () -> Unit) {
    val text = remember(diffEntry) {
        gitManager.diffFormat(diffEntry)
    }

    Card(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        Column {
            OutlinedButton(
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .align(Alignment.End),
                onClick = onCloseDiffView,
            ) {
                Text("Close diff")
            }
            val textLines = text.split("\n", "\r\n")
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                items(textLines) { line ->
                    val backgroundColor = if (line.startsWith("+")) {
                        Color(0x77a9d49b)
                    } else if (line.startsWith("-")) {
                        Color(0x77dea2a2)
                    } else {
                        MaterialTheme.colors.surface
                    }

                    SelectionContainer {
                        Text(
                            text = line,
                            modifier = Modifier
                                .background(backgroundColor)
                                .fillMaxWidth(),
                            color = MaterialTheme.colors.primaryTextColor,
                            maxLines = 1,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

        }
    }
}

