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
import androidx.compose.ui.unit.sp
import theme.primaryTextColor

@Composable
fun Diff(gitManager: GitManager, diffEntryType: DiffEntryType, onCloseDiffView: () -> Unit) {
    val text = remember(diffEntryType.diffEntry) {
        gitManager.diffFormat(diffEntryType)
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
            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(text) { line ->
                        val isHunkLine = line.startsWith("@@") && line.endsWith("@@")

                        val backgroundColor = when {
                            line.startsWith("+") -> {
                                Color(0x77a9d49b)
                            }
                            line.startsWith("-") -> {
                                Color(0x77dea2a2)
                            }
                            isHunkLine -> {
                                MaterialTheme.colors.background
                            }
                            else -> {
                                MaterialTheme.colors.surface
                            }
                        }

                        val paddingTop = if (isHunkLine)
                            32.dp
                        else
                            0.dp

                        Text(
                            text = line,
                            modifier = Modifier
                                .padding(top = paddingTop)
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

